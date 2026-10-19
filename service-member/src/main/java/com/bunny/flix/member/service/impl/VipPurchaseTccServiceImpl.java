package com.bunny.flix.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bunny.flix.core.exception.BusinessException;
import com.bunny.flix.member.entity.UserWallet;
import com.bunny.flix.member.entity.VipOrder;
import com.bunny.flix.member.entity.VipPlan;
import com.bunny.flix.member.mapper.UserWalletMapper;
import com.bunny.flix.member.mapper.VipOrderMapper;
import com.bunny.flix.member.mapper.VipPlanMapper;
import com.bunny.flix.member.service.VipPurchaseTccService;
import com.bunny.flix.member.service.VipService;
import io.seata.rm.tcc.api.BusinessActionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * VIP 购买 TCC 分布式事务实现
 *
 * 订单状态机（status）：
 *   0 待支付   —— 订单已创建，Try 未执行
 *   3 处理中   —— Try 已执行，金币已冻结
 *   1 已完成   —— Confirm 已执行
 *   2 已取消   —— Cancel 已执行
 *
 * 状态机是处理 TCC 三大坑的关键：通过判断当前状态，就能知道
 * "Try 到底执行过没有"、"二阶段是否重复执行"。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VipPurchaseTccServiceImpl implements VipPurchaseTccService {

    /** 订单状态：待支付（未 Try） */
    private static final int STATUS_INIT = 0;
    /** 订单状态：处理中（Try 已完成，金币已冻结） */
    private static final int STATUS_TRYING = 3;
    /** 订单状态：已完成（Confirm） */
    private static final int STATUS_CONFIRMED = 1;
    /** 订单状态：已取消（Cancel） */
    private static final int STATUS_CANCELLED = 2;

    private final UserWalletMapper userWalletMapper;
    private final VipOrderMapper vipOrderMapper;
    private final VipPlanMapper vipPlanMapper;
    private final VipService vipService;

    // ==================================================================
    // Try：冻结金币（资源预留）
    // ==================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean tryPurchase(String orderNo, Long userId, Long planId) {
        log.info("TCC Try 开始: orderNo={}, userId={}, planId={}", orderNo, userId, planId);

        VipOrder order = getOrder(orderNo);
        if (order == null) {
            throw new BusinessException(4001, "订单不存在");
        }

        // 【幂等】Try 已执行过，直接返回成功，避免重复冻结
        if (order.getStatus() == STATUS_TRYING) {
            log.info("TCC Try 已执行过，幂等返回: orderNo={}", orderNo);
            return true;
        }

        // 【防悬挂】Cancel 先于 Try 到达（网络延迟导致），必须拒绝执行 Try，
        // 否则冻结的金额再也没有 Cancel 来释放，变成悬挂资源
        if (order.getStatus() == STATUS_CANCELLED) {
            log.warn("TCC Try 拒绝执行：订单已取消（防悬挂）: orderNo={}", orderNo);
            throw new BusinessException(4003, "订单已取消，无法继续支付");
        }

        VipPlan plan = vipPlanMapper.selectById(planId);
        if (plan == null || plan.getEnabled() == 0) {
            throw new BusinessException("VIP套餐不存在或已下架");
        }

        Long amount = plan.getPrice().longValue();
        UserWallet wallet = getWallet(userId);
        if (wallet == null || wallet.getBalance() < amount) {
            throw new BusinessException(4002, "余额不足");
        }

        // 冻结：余额扣减，冻结额增加（乐观锁防并发）
        LambdaUpdateWrapper<UserWallet> update = new LambdaUpdateWrapper<>();
        update.eq(UserWallet::getUserId, userId)
                .eq(UserWallet::getVersion, wallet.getVersion())
                .setSql("balance = balance - " + amount)
                .setSql("frozen_balance = frozen_balance + " + amount)
                .set(UserWallet::getVersion, wallet.getVersion() + 1)
                .set(UserWallet::getUpdateTime, LocalDateTime.now());

        int rows = userWalletMapper.update(null, update);
        if (rows <= 0) {
            // 乐观锁冲突：由 Seata 重试 Try
            log.warn("TCC Try 冻结失败（乐观锁冲突）: orderNo={}", orderNo);
            throw new BusinessException(4004, "操作过于频繁，请重试");
        }

        // 状态置为「处理中」，标记 Try 已完成
        updateStatus(order, STATUS_TRYING);

        log.info("TCC Try 成功: orderNo={}, 冻结金额={}", orderNo, amount);
        return true;
    }

    // ==================================================================
    // Confirm：扣减冻结额 + 开通 VIP
    // ==================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean confirmPurchase(BusinessActionContext context) {
        String orderNo = getOrderNo(context);
        log.info("TCC Confirm 开始: orderNo={}", orderNo);

        VipOrder order = getOrder(orderNo);
        if (order == null) {
            log.warn("TCC Confirm 订单不存在，忽略: orderNo={}", orderNo);
            return true;
        }

        // 【幂等】已 Confirm 过，直接返回，避免重复开通 VIP
        if (order.getStatus() == STATUS_CONFIRMED) {
            log.info("TCC Confirm 已执行过，幂等返回: orderNo={}", orderNo);
            return true;
        }

        Long amount = order.getAmount() == null ? 0L : order.getAmount().longValue();
        UserWallet wallet = getWallet(order.getUserId());
        if (wallet != null && amount > 0) {
            // 扣减冻结额（真正完成扣款）
            LambdaUpdateWrapper<UserWallet> update = new LambdaUpdateWrapper<>();
            update.eq(UserWallet::getUserId, order.getUserId())
                    .eq(UserWallet::getVersion, wallet.getVersion())
                    .setSql("frozen_balance = frozen_balance - " + amount)
                    .set(UserWallet::getVersion, wallet.getVersion() + 1)
                    .set(UserWallet::getUpdateTime, LocalDateTime.now());

            int rows = userWalletMapper.update(null, update);
            if (rows <= 0) {
                // Confirm 失败必须抛异常，让 Seata 持续重试，否则会造成资损
                log.error("TCC Confirm 扣减冻结额失败，等待重试: orderNo={}", orderNo);
                throw new BusinessException(4005, "确认扣款失败，等待重试");
            }
        }

        // 开通 VIP
        vipService.activateVip(order.getUserId(), order.getPlanId(), 1);

        // 订单置为已完成
        order.setStatus(STATUS_CONFIRMED);
        order.setPayTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        vipOrderMapper.updateById(order);

        log.info("TCC Confirm 成功: orderNo={}", orderNo);
        return true;
    }

    // ==================================================================
    // Cancel：解冻金币
    // ==================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelPurchase(BusinessActionContext context) {
        String orderNo = getOrderNo(context);
        log.info("TCC Cancel 开始: orderNo={}", orderNo);

        VipOrder order = getOrder(orderNo);
        if (order == null) {
            log.warn("TCC Cancel 订单不存在，忽略: orderNo={}", orderNo);
            return true;
        }

        // 【幂等】已 Cancel 过
        if (order.getStatus() == STATUS_CANCELLED) {
            log.info("TCC Cancel 已执行过，幂等返回: orderNo={}", orderNo);
            return true;
        }

        // 【空回滚】Try 没执行过（状态仍是「待支付」），不能解冻！
        // 因为没有冻结过，解冻会凭空给用户增加金币。只把订单置为取消即可。
        if (order.getStatus() != STATUS_TRYING) {
            log.warn("TCC Cancel 空回滚：Try 未执行，仅置状态: orderNo={}, status={}",
                    orderNo, order.getStatus());
            order.setStatus(STATUS_CANCELLED);
            order.setUpdateTime(LocalDateTime.now());
            vipOrderMapper.updateById(order);
            return true;
        }

        // 正常回滚：解冻金币
        Long amount = order.getAmount() == null ? 0L : order.getAmount().longValue();
        UserWallet wallet = getWallet(order.getUserId());
        if (wallet != null && amount > 0) {
            LambdaUpdateWrapper<UserWallet> update = new LambdaUpdateWrapper<>();
            update.eq(UserWallet::getUserId, order.getUserId())
                    .eq(UserWallet::getVersion, wallet.getVersion())
                    .setSql("balance = balance + " + amount)
                    .setSql("frozen_balance = frozen_balance - " + amount)
                    .set(UserWallet::getVersion, wallet.getVersion() + 1)
                    .set(UserWallet::getUpdateTime, LocalDateTime.now());

            int rows = userWalletMapper.update(null, update);
            if (rows <= 0) {
                // Cancel 失败同样抛异常，交给 Seata 重试，否则冻结金额永远释放不了
                log.error("TCC Cancel 解冻失败，等待重试: orderNo={}", orderNo);
                throw new BusinessException(4006, "解冻失败，等待重试");
            }
        }

        order.setStatus(STATUS_CANCELLED);
        order.setUpdateTime(LocalDateTime.now());
        vipOrderMapper.updateById(order);

        log.info("TCC Cancel 成功: orderNo={}", orderNo);
        return true;
    }

    // ==================================================================
    // 内部方法
    // ==================================================================

    /** 从二阶段上下文中取回订单号 */
    private String getOrderNo(BusinessActionContext context) {
        if (context == null || context.getActionContext("orderNo") == null) {
            throw new BusinessException(4007, "TCC上下文缺少订单号");
        }
        return String.valueOf(context.getActionContext("orderNo"));
    }

    private VipOrder getOrder(String orderNo) {
        return vipOrderMapper.selectOne(
                new LambdaQueryWrapper<VipOrder>().eq(VipOrder::getOrderNo, orderNo));
    }

    private UserWallet getWallet(Long userId) {
        return userWalletMapper.selectOne(
                new LambdaQueryWrapper<UserWallet>().eq(UserWallet::getUserId, userId));
    }

    private void updateStatus(VipOrder order, int status) {
        order.setStatus(status);
        order.setUpdateTime(LocalDateTime.now());
        vipOrderMapper.updateById(order);
    }
}
