package com.bunny.flix.member.service;

import com.bunny.flix.member.entity.UserWallet;

public interface WalletService {

    UserWallet getWallet(Long userId);

    Long getBalance(Long userId);

    boolean addBalance(Long userId, Long amount, String orderNo);

    boolean deductBalance(Long userId, Long amount, String orderNo);

    boolean refundBalance(Long userId, Long amount, String orderNo);
}
