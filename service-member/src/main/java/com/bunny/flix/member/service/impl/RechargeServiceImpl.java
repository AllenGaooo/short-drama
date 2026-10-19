package com.bunny.flix.member.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bunny.flix.core.exception.BusinessException;
import com.bunny.flix.member.entity.RechargeOrder;
import com.bunny.flix.member.mapper.RechargeOrderMapper;
import com.bunny.flix.member.service.RechargeService;
import com.bunny.flix.member.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RechargeServiceImpl implements RechargeService {

    private final RechargeOrderMapper rechargeOrderMapper;
    private final WalletService walletService;

    // 金币充值比例: 1元 = 10金币
    private static final int COIN_RATIO = 10;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RechargeOrder createRechargeOrder(Long userId, Long amount) {
        String orderNo = IdUtil.getSnowflakeNextIdStr();
        int money = (int) (amount / COIN_RATIO);

        RechargeOrder order = new RechargeOrder();
        order.setUserId(userId);
        order.setOrderNo(orderNo);
        order.setAmount(amount);
        order.setMoney(money);
        order.setStatus(0);
        order.setCreateTime(LocalDateTime.now());
        rechargeOrderMapper.insert(order);

        log.info("充值订单创建成功: orderNo={}, userId={}, amount={}", orderNo, userId, amount);
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handlePayCallback(String orderNo, boolean success) {
        LambdaQueryWrapper<RechargeOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RechargeOrder::getOrderNo, orderNo);
        RechargeOrder order = rechargeOrderMapper.selectOne(wrapper);

        if (order == null) {
            throw new BusinessException(4001, "订单不存在");
        }

        if (order.getStatus() != 0) {
            log.warn("订单状态异常: orderNo={}, status={}", orderNo, order.getStatus());
            return;
        }

        if (success) {
            order.setStatus(1);
            order.setUpdateTime(LocalDateTime.now());
            rechargeOrderMapper.updateById(order);

            // 增加用户金币
            walletService.addBalance(order.getUserId(), order.getAmount(), orderNo);
            log.info("充值支付成功: orderNo={}, userId={}, amount={}", orderNo, order.getUserId(), order.getAmount());
        } else {
            order.setStatus(2);
            order.setUpdateTime(LocalDateTime.now());
            rechargeOrderMapper.updateById(order);
            log.info("充值支付失败: orderNo={}", orderNo);
        }
    }

    @Override
    public List<RechargeOrder> getRechargeOrders(Long userId, Integer page, Integer size) {
        Page<RechargeOrder> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<RechargeOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RechargeOrder::getUserId, userId)
                .orderByDesc(RechargeOrder::getCreateTime);

        Page<RechargeOrder> result = rechargeOrderMapper.selectPage(pageParam, wrapper);
        return result.getRecords();
    }
}
