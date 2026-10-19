package com.bunny.flix.member.service;

import com.bunny.flix.member.entity.VipPlan;
import com.bunny.flix.member.entity.VipRecord;

import java.util.List;

public interface VipService {

    VipRecord getUserVipStatus(Long userId);

    String purchaseVip(Long userId, Long planId);

    void activateVip(Long userId, Long planId, Integer source);

    void checkVipExpiry();

    List<VipPlan> getVipPlans();
}
