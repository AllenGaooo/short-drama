package com.bunny.flix.security;

import lombok.Data;

/**
 * 当前登录用户上下文
 * 使用 ThreadLocal 存储当前请求的用户信息
 * 网关把Token中的用户信息传到下游服务后，下游服务存到这里，方便在代码中随时获取
 *
 * 使用流程：
 * 1. 过滤器把用户信息 set 进来
 * 2. 业务代码里通过 get() 获取
 * 3. 请求结束时 remove() 清理，防止内存泄漏
 */
@Data
public class UserContext {

    /** ThreadLocal 保证每个请求线程都有自己的用户信息，互不干扰 */
    private static final ThreadLocal<UserContext> CONTEXT = new ThreadLocal<>();

    private Long userId;
    private String phone;
    private String userType;
    private String token;

    /** 把当前用户信息存入当前线程 */
    public static void set(UserContext context) {
        CONTEXT.set(context);
    }

    /** 获取当前线程存储的用户信息 */
    public static UserContext get() {
        return CONTEXT.get();
    }

    /** 清理当前线程的用户信息，防止内存泄漏 */
    public static void remove() {
        CONTEXT.remove();
    }

    /** 快捷获取当前登录用户ID（注意：内部直接读 userId 字段，避免调用与 Lombok 实例 getter 同名的 getUserId() 形成递归） */
    public static Long getUserId() {
        UserContext context = CONTEXT.get();
        return context != null ? context.userId : null;
    }
}
