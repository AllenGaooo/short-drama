package com.bunny.flix.user.service;

public interface SmsService {

    /**
     * 发送验证码
     */
    void sendCode(String phone);

    /**
     * 验证验证码
     */
    boolean verifyCode(String phone, String code);
}
