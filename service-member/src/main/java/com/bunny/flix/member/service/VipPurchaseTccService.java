package com.bunny.flix.member.service;

import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;

/**
 * VIP 购买 TCC 分布式事务接口
 *
 * TCC 三阶段：
 *   Try    —— 资源预留：冻结用户钱包金币（不真正扣款）
 *   Confirm—— 提交：扣减冻结金额、开通 VIP、订单置为已完成
 *   Cancel —— 回滚：解冻金币、订单置为已取消
 *
 * 二阶段（Confirm/Cancel）由 Seata TC 在全局事务提交/回滚时自动回调，
 * 因此方法签名必须是 boolean xxx(BusinessActionContext context)。
 *
 * TCC 三大坑在实现类中处理：
 *   1. 幂等    —— Confirm/Cancel 可能被重复调用，用订单状态去重
 *   2. 空回滚  —— Try 未执行却收到 Cancel，不能解冻（否则凭空多出金币）
 *   3. 防悬挂  —— Cancel 先于 Try 到达，Try 必须拒绝执行（否则冻结金额永远无法释放）
 */
@LocalTCC
public interface VipPurchaseTccService {

    /**
     * Try 阶段：冻结金币
     *
     * @param orderNo 订单号（通过 @BusinessActionContextParameter 透传到二阶段）
     * @param userId  用户ID
     * @param planId  套餐ID
     */
    @TwoPhaseBusinessAction(name = "purchaseVipTcc",
            commitMethod = "confirmPurchase",
            rollbackMethod = "cancelPurchase")
    boolean tryPurchase(@BusinessActionContextParameter(paramName = "orderNo") String orderNo,
                        @BusinessActionContextParameter(paramName = "userId") Long userId,
                        Long planId);

    /** Confirm 阶段：提交 */
    boolean confirmPurchase(BusinessActionContext context);

    /** Cancel 阶段：回滚 */
    boolean cancelPurchase(BusinessActionContext context);
}
