package com.bunny.flix.member.controller;

import com.bunny.flix.core.Result;
import com.bunny.flix.member.entity.UserWallet;
import com.bunny.flix.member.service.WalletService;
import com.bunny.flix.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/member/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping
    public Result<UserWallet> getWallet() {
        Long userId = UserContext.getUserId();
        UserWallet wallet = walletService.getWallet(userId);
        return Result.success(wallet);
    }

    @GetMapping("/balance")
    public Result<Map<String, Long>> getBalance() {
        Long userId = UserContext.getUserId();
        Long balance = walletService.getBalance(userId);
        return Result.success(Map.of("balance", balance));
    }
}
