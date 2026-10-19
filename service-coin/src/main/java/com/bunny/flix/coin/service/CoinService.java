package com.bunny.flix.coin.service;

/**
 * 金币服务
 */
public interface CoinService {

    /**
     * 发放看剧奖励
     *
     * @param userId    用户ID
     * @param episodeId 剧集ID（幂等维度）
     * @param dramaId   短剧ID（流水记录用）
     * @return 是否实际发放（false 表示被幂等拦截或超出限额）
     */
    boolean rewardForWatch(Long userId, Long episodeId, Long dramaId);

    /** 查询余额 */
    Integer getBalance(Long userId);

    /** 查询今日已发放数量（用于限额判断） */
    Integer getTodayEarned(Long userId);
}
