package com.bunny.flix.coin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bunny.flix.coin.entity.CoinAccount;
import com.bunny.flix.coin.entity.CoinRecord;
import com.bunny.flix.coin.mapper.CoinAccountMapper;
import com.bunny.flix.coin.mapper.CoinRecordMapper;
import com.bunny.flix.coin.service.CoinService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.TimeUnit;

/**
 * 金币服务实现
 *
 * 防重复发币的三道防线：
 * 1. 【Redis SETNX 幂等】reward:{userId}:{episodeId} 抢占发放资格，拦截 MQ 重复投递与用户重复请求
 * 2. 【数据库唯一索引】t_coin_record 的 uk_user_biz(userId, bizType, bizId) 保证同一剧集只产生一条奖励流水
 * 3. 【每日限额】Redis 计数 daily:{userId}:{date}，超过上限不再发放，防刷
 *
 * 余额更新使用乐观锁（version），避免并发更新导致余额错乱。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CoinServiceImpl implements CoinService {

    /** 业务类型：看剧奖励 */
    private static final String BIZ_WATCH_REWARD = "WATCH_REWARD";

    private final CoinAccountMapper accountMapper;
    private final CoinRecordMapper recordMapper;
    private final StringRedisTemplate redisTemplate;

    /** 每集奖励金币数 */
    @Value("${coin.reward-per-episode:10}")
    private int rewardPerEpisode;

    /** 每日发放上限 */
    @Value("${coin.daily-limit:200}")
    private int dailyLimit;

    /** 幂等标记过期天数 */
    @Value("${coin.idempotent-expire-days:30}")
    private long idempotentExpireDays;

    @Override
    @Transactional
    public boolean rewardForWatch(Long userId, Long episodeId, Long dramaId) {
        if (userId == null || episodeId == null) {
            log.warn("[coin] 参数不完整 userId={} episodeId={}", userId, episodeId);
            return false;
        }

        // 防线1：Redis SETNX 幂等抢占（快速拦截绝大部分重复请求）
        String rewardKey = rewardKey(userId, episodeId);
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(rewardKey, String.valueOf(System.currentTimeMillis()),
                        idempotentExpireDays, TimeUnit.DAYS);
        if (!Boolean.TRUE.equals(acquired)) {
            log.info("[coin] 奖励已发放过，幂等拦截 userId={} episodeId={}", userId, episodeId);
            return false;
        }

        // 防线3：每日限额校验（防刷）
        if (isOverDailyLimit(userId)) {
            log.warn("[coin] 超出每日发放上限 userId={} limit={}", userId, dailyLimit);
            // 限额触发时释放幂等标记，允许用户次日重新获得
            redisTemplate.delete(rewardKey);
            return false;
        }

        // 发放金币（防线2：唯一索引兜底，重复插入会抛异常触发回滚）
        try {
            int newBalance = increaseBalance(userId, rewardPerEpisode);
            recordFlow(userId, rewardPerEpisode, newBalance, BIZ_WATCH_REWARD,
                    String.valueOf(episodeId), "看剧奖励·剧集" + episodeId);
            incrDailyEarned(userId, rewardPerEpisode);
            log.info("[coin] 发放看剧奖励成功 userId={} episodeId={} amount={} balance={}",
                    userId, episodeId, rewardPerEpisode, newBalance);
            return true;
        } catch (Exception e) {
            // 发放失败：释放幂等标记，让消息可以重投后重试
            redisTemplate.delete(rewardKey);
            log.error("[coin] 发放奖励失败，已释放幂等标记 userId={} episodeId={}: {}",
                    userId, episodeId, e.getMessage());
            throw e;
        }
    }

    @Override
    public Integer getBalance(Long userId) {
        CoinAccount account = getOrCreateAccount(userId);
        return account.getBalance();
    }

    @Override
    public Integer getTodayEarned(Long userId) {
        Object v = redisTemplate.opsForValue().get(dailyKey(userId));
        return v == null ? 0 : Integer.parseInt(v.toString());
    }

    // ---------------- 内部方法 ----------------

    /**
     * 增加余额：乐观锁更新，失败自动重试
     * 账户不存在时先初始化
     */
    private int increaseBalance(Long userId, int amount) {
        for (int i = 0; i < 3; i++) {
            CoinAccount account = getOrCreateAccount(userId);
            int currentVersion = account.getVersion() == null ? 0 : account.getVersion();
            int newBalance = account.getBalance() + amount;

            LambdaUpdateWrapper<CoinAccount> update = new LambdaUpdateWrapper<>();
            update.eq(CoinAccount::getUserId, userId)
                    .eq(CoinAccount::getVersion, currentVersion)
                    .set(CoinAccount::getBalance, newBalance)
                    .set(CoinAccount::getTotalEarned, account.getTotalEarned() + amount)
                    .set(CoinAccount::getVersion, currentVersion + 1)
                    .set(CoinAccount::getUpdateTime, LocalDateTime.now());

            int updated = accountMapper.update(null, update);
            if (updated > 0) {
                return newBalance;
            }
            log.warn("[coin] 余额更新乐观锁冲突，重试 {}/3 userId={}", i + 1, userId);
        }
        throw new IllegalStateException("余额更新失败，请重试: userId=" + userId);
    }

    /** 查询账户，不存在则初始化 */
    private CoinAccount getOrCreateAccount(Long userId) {
        CoinAccount account = accountMapper.selectOne(
                new LambdaQueryWrapper<CoinAccount>().eq(CoinAccount::getUserId, userId));
        if (account != null) {
            return account;
        }
        account = new CoinAccount();
        account.setUserId(userId);
        account.setBalance(0);
        account.setTotalEarned(0);
        account.setTotalSpent(0);
        account.setVersion(0);
        account.setCreateTime(LocalDateTime.now());
        account.setUpdateTime(LocalDateTime.now());
        try {
            accountMapper.insert(account);
        } catch (Exception e) {
            // 并发下可能已被其他线程创建，重新查询
            account = accountMapper.selectOne(
                    new LambdaQueryWrapper<CoinAccount>().eq(CoinAccount::getUserId, userId));
        }
        return account;
    }

    /** 记录流水 */
    private void recordFlow(Long userId, int amount, int balanceAfter,
                            String bizType, String bizId, String remark) {
        CoinRecord record = new CoinRecord();
        record.setUserId(userId);
        record.setAmount(amount);
        record.setBalanceAfter(balanceAfter);
        record.setBizType(bizType);
        record.setBizId(bizId);
        record.setRemark(remark);
        record.setCreateTime(LocalDateTime.now());
        recordMapper.insert(record);
    }

    /** 累计今日发放（Redis 计数，跨自然日自动失效） */
    private void incrDailyEarned(Long userId, int amount) {
        String key = dailyKey(userId);
        redisTemplate.opsForValue().increment(key, amount);
        // 设置到今日 23:59:59 过期
        LocalDateTime endOfDay = LocalDateTime.now().with(LocalTime.MAX);
        long seconds = java.time.Duration.between(LocalDateTime.now(), endOfDay).getSeconds();
        redisTemplate.expire(key, Math.max(seconds, 1), TimeUnit.SECONDS);
    }

    private boolean isOverDailyLimit(Long userId) {
        return getTodayEarned(userId) + rewardPerEpisode > dailyLimit;
    }

    public static String rewardKey(Long userId, Long episodeId) {
        return "reward:" + userId + ":" + episodeId;
    }

    private String dailyKey(Long userId) {
        return "coin:daily:" + userId + ":" + LocalDate.now();
    }
}
