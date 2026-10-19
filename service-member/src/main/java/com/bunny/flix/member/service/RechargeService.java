package com.bunny.flix.member.service;

import com.bunny.flix.member.entity.RechargeOrder;

import java.util.List;

public interface RechargeService {

    RechargeOrder createRechargeOrder(Long userId, Long amount);

    void handlePayCallback(String orderNo, boolean success);

    List<RechargeOrder> getRechargeOrders(Long userId, Integer page, Integer size);
}
