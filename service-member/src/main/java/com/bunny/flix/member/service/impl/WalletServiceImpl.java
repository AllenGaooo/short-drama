package com.bunny.flix.member.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bunny.flix.core.exception.BusinessException;
import com.bunny.flix.member.entity.UserWallet;
import com.bunny.flix.member.entity.WalletTransaction;
import com.bunny.flix.member.mapper.UserWalletMapper;
import com.bunny.flix.member.mapper.WalletTransactionMapper;
import com.bunny.flix.member.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final UserWalletMapper userWalletMapper;
    private final WalletTransactionMapper walletTransactionMapper;
    private final RedissonClient redissonClient;

    private static final String WALLET_LOCK_KEY = "wallet:lock:";

    @Override
    public UserWallet getWallet(Long userId) {
        UserWallet wallet = getOrCreateWallet(userId);
        return wallet;
    }

    @Override
    public Long getBalance(Long userId) {
        UserWallet wallet = getOrCreateWallet(userId);
        return wallet.getBalance();
    }

    @Override
    public boolean addBalance(Long userId, Long amount, String orderNo) {
        String lockKey = WALLET_LOCK_KEY + userId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (lock.tryLock(10, 30, TimeUnit.SECONDS)) {
                try {
                    return doAddBalance(userId, amount, orderNo);
                } finally {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("获取锁失败");
        }
        return false;
    }

    @Override
    public boolean deductBalance(Long userId, Long amount, String orderNo) {
        String lockKey = WALLET_LOCK_KEY + userId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (lock.tryLock(10, 30, TimeUnit.SECONDS)) {
                try {
                    return doDeductBalance(userId, amount, orderNo);
                } finally {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("获取锁失败");
        }
        return false;
    }

    @Override
    public boolean refundBalance(Long userId, Long amount, String orderNo) {
        return addBalance(userId, amount, orderNo);
    }

    @Transactional(rollbackFor = Exception.class)
    private boolean doAddBalance(Long userId, Long amount, String orderNo) {
        UserWallet wallet = getOrCreateWallet(userId);
        Long beforeBalance = wallet.getBalance();
        Long afterBalance = beforeBalance + amount;

        LambdaUpdateWrapper<UserWallet> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(UserWallet::getUserId, userId)
                .eq(UserWallet::getVersion, wallet.getVersion())
                .set(UserWallet::getBalance, afterBalance)
                .set(UserWallet::getVersion, wallet.getVersion() + 1)
                .set(UserWallet::getUpdateTime, LocalDateTime.now());

        int rows = userWalletMapper.update(null, wrapper);
        if (rows <= 0) {
            log.warn("增加金币失败(乐观锁冲突): userId={}, amount={}", userId, amount);
            return false;
        }

        recordTransaction(userId, amount, beforeBalance, afterBalance, "recharge", orderNo);
        log.info("增加金币成功: userId={}, amount={}, afterBalance={}", userId, amount, afterBalance);
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    private boolean doDeductBalance(Long userId, Long amount, String orderNo) {
        UserWallet wallet = getOrCreateWallet(userId);
        if (wallet.getBalance() < amount) {
            throw new BusinessException(4002, "余额不足");
        }

        Long beforeBalance = wallet.getBalance();
        Long afterBalance = beforeBalance - amount;

        LambdaUpdateWrapper<UserWallet> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(UserWallet::getUserId, userId)
                .eq(UserWallet::getVersion, wallet.getVersion())
                .set(UserWallet::getBalance, afterBalance)
                .set(UserWallet::getVersion, wallet.getVersion() + 1)
                .set(UserWallet::getUpdateTime, LocalDateTime.now());

        int rows = userWalletMapper.update(null, wrapper);
        if (rows <= 0) {
            log.warn("扣减金币失败(乐观锁冲突): userId={}, amount={}", userId, amount);
            return false;
        }

        recordTransaction(userId, amount, beforeBalance, afterBalance, "consume", orderNo);
        log.info("扣减金币成功: userId={}, amount={}, afterBalance={}", userId, amount, afterBalance);
        return true;
    }

    private void recordTransaction(Long userId, Long amount, Long beforeBalance,
                                   Long afterBalance, String type, String orderNo) {
        WalletTransaction transaction = new WalletTransaction();
        transaction.setUserId(userId);
        transaction.setAmount(amount);
        transaction.setBeforeBalance(beforeBalance);
        transaction.setAfterBalance(afterBalance);
        transaction.setType(type);
        transaction.setOrderNo(orderNo);
        transaction.setCreateTime(LocalDateTime.now());
        walletTransactionMapper.insert(transaction);
    }

    private UserWallet getOrCreateWallet(Long userId) {
        LambdaQueryWrapper<UserWallet> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserWallet::getUserId, userId);
        UserWallet wallet = userWalletMapper.selectOne(wrapper);

        if (wallet == null) {
            wallet = new UserWallet();
            wallet.setUserId(userId);
            wallet.setBalance(0L);
            wallet.setFrozenBalance(0L);
            wallet.setVersion(0);
            wallet.setCreateTime(LocalDateTime.now());
            wallet.setUpdateTime(LocalDateTime.now());
            userWalletMapper.insert(wallet);
        }
        return wallet;
    }
}
