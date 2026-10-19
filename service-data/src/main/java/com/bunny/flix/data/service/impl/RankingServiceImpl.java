package com.bunny.flix.data.service.impl;

import cn.hutool.json.JSONUtil;
import com.bunny.flix.data.entity.RankingCache;
import com.bunny.flix.data.service.RankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankingServiceImpl implements RankingService {

    private final StringRedisTemplate redisTemplate;

    private static final String HOT_RANKING_KEY = "ranking:hot";
    private static final String NEW_RANKING_KEY = "ranking:new";
    private static final String CATEGORY_RANKING_PREFIX = "ranking:category:";

    @Override
    public List<RankingCache> getHotRanking(Integer page, Integer size) {
        int start = (page - 1) * size;
        int end = start + size - 1;

        Set<String> entries = redisTemplate.opsForZSet()
                .reverseRange(HOT_RANKING_KEY, start, end);

        if (entries == null || entries.isEmpty()) {
            return Collections.emptyList();
        }

        List<RankingCache> result = new ArrayList<>();
        int rank = start + 1;
        for (String entry : entries) {
            RankingCache cache = JSONUtil.toBean(entry, RankingCache.class);
            cache.setRank(rank++);
            result.add(cache);
        }
        return result;
    }

    @Override
    public List<RankingCache> getNewRanking(Integer page, Integer size) {
        int start = (page - 1) * size;
        int end = start + size - 1;

        Set<String> entries = redisTemplate.opsForZSet()
                .reverseRange(NEW_RANKING_KEY, start, end);

        if (entries == null || entries.isEmpty()) {
            return Collections.emptyList();
        }

        List<RankingCache> result = new ArrayList<>();
        int rank = start + 1;
        for (String entry : entries) {
            RankingCache cache = JSONUtil.toBean(entry, RankingCache.class);
            cache.setRank(rank++);
            result.add(cache);
        }
        return result;
    }

    @Override
    public List<RankingCache> getCategoryRanking(Long tagId, Integer page, Integer size) {
        String key = CATEGORY_RANKING_PREFIX + tagId;
        int start = (page - 1) * size;
        int end = start + size - 1;

        Set<String> entries = redisTemplate.opsForZSet()
                .reverseRange(key, start, end);

        if (entries == null || entries.isEmpty()) {
            return Collections.emptyList();
        }

        List<RankingCache> result = new ArrayList<>();
        int rank = start + 1;
        for (String entry : entries) {
            RankingCache cache = JSONUtil.toBean(entry, RankingCache.class);
            cache.setRank(rank++);
            result.add(cache);
        }
        return result;
    }

    @Override
    public void refreshRankings() {
        log.info("Refreshing rankings...");
        // This is a simplified implementation.
        // In production, this would query actual data sources (DB, ES, etc.)
        // and rebuild the Redis ZSET rankings.
        // For now, we log the refresh event.
        log.info("Rankings refresh completed.");
    }
}
