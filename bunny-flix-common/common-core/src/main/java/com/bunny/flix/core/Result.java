package com.bunny.flix.core;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 统一响应结果类
 * 所有接口都通过这个类返回给前端，方便前端统一处理
 *
 * @param <T> 返回数据的类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    /** 状态码，200表示成功，其他表示各种错误 */
    private Integer code;
    /** 提示信息，告诉用户操作结果 */
    private String message;
    /** 返回的数据，可以是任意类型 */
    private T data;

    /** 操作成功，没有数据返回 */
    public static <T> Result<T> success() {
        return new Result<>(200, "success", null);
    }

    /** 操作成功，返回数据 */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }

    /** 操作成功，仅自定义提示信息，无业务数据返回（如短信发送成功） */
    public static <T> Result<T> success(String message) {
        return new Result<>(200, message, null);
    }

    /** 操作成功，自定义提示信息并返回数据 */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }

    /** 操作失败，指定错误码和错误信息 */
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    /** 操作失败，默认500错误码 */
    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }

    /** 未登录或登录过期，401错误 */
    public static <T> Result<T> unauthorized(String message) {
        return new Result<>(401, message, null);
    }

    /** 没有权限访问，403错误 */
    public static <T> Result<T> forbidden(String message) {
        return new Result<>(403, message, null);
    }

    /** 资源不存在，404错误 */
    public static <T> Result<T> notFound(String message) {
        return new Result<>(404, message, null);
    }

    /** 判断本次请求是否成功 */
    public boolean isSuccess() {
        return code != null && code == 200;
    }
}
