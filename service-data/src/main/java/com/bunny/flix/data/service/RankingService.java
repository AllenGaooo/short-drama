package com.bunny.flix.data.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankingService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String HOT_RANK_KEY = "ranking:hot";
    private static final String NEW_RANK_KEY = "ranking:new";

    public List<Map<String, Object>> getHotRanking(int page, int size) {
        return getRanking(HOT_RANK_KEY, page, size, true);
    }

    public List<Map<String, Object>> getNewRanking(int page, int size) {
        return getRanking(NEW_RANK_KEY, page, size, true);
    }

    public List<Map<String, Object>> getCategoryRanking(Long tagId, int page, int size) {
        String key = "ranking:category:" + tagId;
        return getRanking(key, page, size, true);
    }

    public long getRankingCount(String key) {
        Long count = redisTemplate.opsForZSet().size(key);
        return count != null ? count : 0;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getRanking(String key, int page, int size, boolean reverse) {
        Set<ZSetOperations.TypedTuple<Object>> tuples;
        if (reverse) {
            tuples = redisTemplate.opsForZSet().reverseRangeWithScores(key, (long) (page - 1) * size, (long) page * size - 1);
        } else {
            tuples = redisTemplate.opsForZSet().rangeWithScores(key, (long) (page - 1) * size, (long) page * size - 1);
        }

        if (tuples == null || tuples.isEmpty()) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        int rank = (page - 1) * size + 1;
        for (ZSetOperations.TypedTuple<Object> tuple : tuples) {
            Map<String, Object> item = new HashMap<>();
            Object value = tuple.getValue();
            if (value instanceof Map) {
                item = new HashMap<>((Map<String, Object>) value);
            } else {
                item.put("dramaId", Long.valueOf(value.toString()));
            }
            item.put("rank", rank++);
            item.put("score", tuple.getScore());
            result.add(item);
        }
        return result;
    }

    @Scheduled(cron = "0 */30 * * * ?")
    public void refreshRankings() {
        log.info("开始刷新排行榜数据...");
        // 实际场景中从数据库加载数据写入Redis ZSET
        log.info("排行榜数据刷新完成");
    }
}
