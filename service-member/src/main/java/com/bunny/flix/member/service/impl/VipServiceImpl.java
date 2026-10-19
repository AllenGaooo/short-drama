package com.bunny.flix.member.service.impl;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bunny.flix.core.exception.BusinessException;
import com.bunny.flix.member.entity.VipOrder;
import com.bunny.flix.member.entity.VipPlan;
import com.bunny.flix.member.entity.VipRecord;
import com.bunny.flix.member.mapper.VipOrderMapper;
import com.bunny.flix.member.mapper.VipPlanMapper;
import com.bunny.flix.member.mapper.VipRecordMapper;
import com.bunny.flix.member.service.VipPurchaseTccService;
import com.bunny.flix.member.service.VipService;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VipServiceImpl implements VipService {

    private final VipRecordMapper vipRecordMapper;
    private final VipPlanMapper vipPlanMapper;
    private final VipOrderMapper vipOrderMapper;

    /**
     * TCC 事务服务。
     * 注意：VipPurchaseTccServiceImpl 内部依赖 VipService（开通VIP），
     * 这里反向注入会形成循环依赖，因此用 @Lazy 延迟加载打破循环。
     */
    @Lazy
    @Autowired
    private VipPurchaseTccService vipPurchaseTccService;

    @Override
    public VipRecord getUserVipStatus(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<VipRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VipRecord::getUserId, userId)
                .le(VipRecord::getStartTime, now)
                .ge(VipRecord::getEndTime, now)
                .orderByDesc(VipRecord::getEndTime)
                .last("LIMIT 1");
        return vipRecordMapper.selectOne(wrapper);
    }

    /**
     * 购买 VIP（分布式事务入口）
     *
     * 用 Seata 的 @GlobalTransactional 开启全局事务，内部调用 TCC 的 Try 阶段：
     *   1. 创建订单（状态：待支付）
     *   2. Try 冻结金币（资源预留）
     *   3. 全局事务提交 → Seata TC 自动回调 Confirm（扣款 + 开通VIP）
     *      任一步失败    → Seata TC 自动回调 Cancel（解冻 + 取消订单）
     */
    @Override
    @GlobalTransactional(name = "purchase-vip", rollbackFor = Exception.class)
    public String purchaseVip(Long userId, Long planId) {
        VipPlan plan = vipPlanMapper.selectById(planId);
        if (plan == null || plan.getEnabled() == 0) {
            throw new BusinessException("VIP套餐不存在或已下架");
        }

        String orderNo = IdUtil.getSnowflakeNextIdStr();
        VipOrder order = new VipOrder();
        order.setUserId(userId);
        order.setPlanId(planId);
        order.setOrderNo(orderNo);
        order.setAmount(plan.getPrice());
        order.setStatus(0);
        order.setCreateTime(LocalDateTime.now());
        vipOrderMapper.insert(order);
        log.info("VIP购买订单创建成功: orderNo={}, userId={}, planId={}", orderNo, userId, planId);

        // Try 阶段：冻结金币（资源预留，不真正扣款）
        // 全局事务提交后由 Seata 自动回调 Confirm 完成扣款与开通
        boolean frozen = vipPurchaseTccService.tryPurchase(orderNo, userId, planId);
        if (!frozen) {
            throw new BusinessException("金币冻结失败，购买失败");
        }

        return orderNo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void activateVip(Long userId, Long planId, Integer source) {
        VipPlan plan = vipPlanMapper.selectById(planId);
        if (plan == null) {
            throw new BusinessException("VIP套餐不存在");
        }

        // 查询当前VIP状态
        VipRecord currentVip = getUserVipStatus(userId);
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime;

        if (currentVip != null) {
            // 已有VIP，在现有结束时间上叠加
            endTime = currentVip.getEndTime().plusDays(plan.getDurationDays());
        } else {
            endTime = startTime.plusDays(plan.getDurationDays());
        }

        VipRecord record = new VipRecord();
        record.setUserId(userId);
        record.setOrderId(null);
        record.setStartTime(startTime);
        record.setEndTime(endTime);
        record.setSource(source);
        record.setCreateTime(LocalDateTime.now());
        vipRecordMapper.insert(record);

        log.info("VIP开通成功: userId={}, planId={}, endTime={}", userId, planId, endTime);
    }

    @Override
    public void checkVipExpiry() {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<VipRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(VipRecord::getEndTime, now);
        List<VipRecord> expiredRecords = vipRecordMapper.selectList(wrapper);

        log.info("扫描到 {} 条过期VIP记录", expiredRecords.size());
        // 实际场景中可以发送通知、更新用户状态等
    }

    @Override
    public List<VipPlan> getVipPlans() {
        LambdaQueryWrapper<VipPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VipPlan::getEnabled, 1)
                .orderByAsc(VipPlan::getPrice);
        return vipPlanMapper.selectList(wrapper);
    }
}
