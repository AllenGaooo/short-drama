package com.bunny.flix.core.exception;

import lombok.Getter;

/**
 * 业务异常类
 * 当业务逻辑出错时（比如用户不存在、余额不足等），抛出这个异常
 * GlobalExceptionHandler 会自动捕获它，并返回给前端对应的错误信息
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 错误码，对应 ResultCode 枚举中的值 */
    private final Integer code;

    /** 自定义错误码和错误信息 */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    /** 默认500错误，只传错误信息 */
    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    /** 使用预定义的错误码枚举 */
    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    /**
     * 预定义的错误码枚举
     * 统一管理项目中所有可能出现的错误码
     */
    public enum ResultCode {
        SUCCESS(200, "操作成功"),
        BAD_REQUEST(400, "请求参数错误"),
        UNAUTHORIZED(401, "未授权"),
        FORBIDDEN(403, "无权限"),
        NOT_FOUND(404, "资源不存在"),
        CONFLICT(409, "资源冲突"),
        INTERNAL_ERROR(500, "服务器内部错误"),
        // 用户相关错误 (1000系列)
        USER_NOT_FOUND(1001, "用户不存在"),
        USER_DISABLED(1002, "用户已冻结"),
        // 短剧相关错误 (2000系列)
        DRAMA_NOT_FOUND(2001, "短剧不存在"),
        EPISODE_NOT_FOUND(2002, "剧集不存在"),
        // 评论相关错误 (3000系列)
        COMMENT_NOT_FOUND(3001, "评论不存在"),
        // 订单/钱包相关错误 (4000系列)
        ORDER_NOT_FOUND(4001, "订单不存在"),
        BALANCE_INSUFFICIENT(4002, "余额不足"),
        VIP_EXPIRED(4003, "VIP已过期"),
        // 优惠券相关错误 (5000系列)
        COUPON_EXPIRED(5001, "优惠券已过期"),
        COUPON_USED(5002, "优惠券已使用"),
        COUPON_STOCK_EMPTY(5003, "优惠券已抢光"),
        // 任务相关错误 (6000系列)
        TASK_NOT_COMPLETED(6001, "任务未完成"),
        TASK_REWARD_CLAIMED(6002, "奖励已领取"),
        ;

        @Getter
        private final Integer code;
        @Getter
        private final String message;

        ResultCode(Integer code, String message) {
            this.code = code;
            this.message = message;
        }
    }
}
