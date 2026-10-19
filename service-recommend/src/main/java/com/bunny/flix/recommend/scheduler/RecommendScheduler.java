package com.bunny.flix.recommend.scheduler;

import cn.hutool.json.JSONUtil;
import com.bunny.flix.recommend.service.RecommendService;
import com.bunny.flix.recommend.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendScheduler {

    private final RecommendService recommendService;
    private final UserProfileService userProfileService;
    private final StringRedisTemplate redisTemplate;

    private static final String RECOMMEND_KEY_PREFIX = "recommend:user:";
    private static final String USER_SIMILARITY_KEY = "user:similarity";

    /**
     * 每天凌晨2点执行：计算热门推荐并缓存，计算用户相似度矩阵
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void executeDailyTask() {
        log.info("推荐定时任务开始执行...");

        try {
            // 1. 计算热门推荐
            log.info("计算热门推荐...");
            List<Map<String, Object>> hotRecommendations = recommendService.calculateHotRecommendations(1, 100);
            log.info("热门推荐计算完成，共 {} 条", hotRecommendations.size());

            // 2. 计算用户相似度矩阵（简化版）
            log.info("计算用户相似度矩阵...");
            calculateUserSimilarityMatrix();
            log.info("用户相似度矩阵计算完成");

            // 3. 为活跃用户更新推荐缓存
            log.info("更新用户推荐缓存...");
            Set<String> activeUserKeys = redisTemplate.keys(USER_SIMILARITY_KEY + ":active:*");
            if (activeUserKeys != null) {
                int count = 0;
                for (String key : activeUserKeys) {
                    try {
                        Long userId = Long.valueOf(key.substring(key.lastIndexOf(":") + 1));
                        List<Map<String, Object>> recommendations = recommendService.calculateContentBased(userId);
                        recommendService.cacheRecommendations(userId, recommendations);
                        count++;
                    } catch (Exception e) {
                        log.warn("更新用户推荐缓存失败: key={}", key, e);
                    }
                }
                log.info("更新了 {} 个用户的推荐缓存", count);
            }

        } catch (Exception e) {
            log.error("推荐定时任务执行异常", e);
        }

        log.info("推荐定时任务执行完成");
    }

    /**
     * 计算用户相似度矩阵（简化版：基于标签偏好余弦相似度）
     */
    private void calculateUserSimilarityMatrix() {
        Set<String> activeUserKeys = redisTemplate.keys(USER_SIMILARITY_KEY + ":active:*");
        if (activeUserKeys == null || activeUserKeys.size() < 2) {
            log.info("活跃用户不足，跳过相似度计算");
            return;
        }

        List<Long> userIds = activeUserKeys.stream()
                .map(key -> {
                    try {
                        return Long.valueOf(key.substring(key.lastIndexOf(":") + 1));
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 批量获取用户标签偏好
        Map<Long, Map<String, Double>> userTagMap = new HashMap<>();
        for (Long userId : userIds) {
            Map<String, Double> tags = userProfileService.getUserTagPreferences(userId);
            if (!tags.isEmpty()) {
                userTagMap.put(userId, tags);
            }
        }

        // 计算相似度并存储
        List<Long> userList = new ArrayList<>(userTagMap.keySet());
        for (int i = 0; i < userList.size(); i++) {
            for (int j = i + 1; j < userList.size(); j++) {
                Long user1 = userList.get(i);
                Long user2 = userList.get(j);

                double similarity = cosineSimilarity(userTagMap.get(user1), userTagMap.get(user2));
                if (similarity > 0.1) {
                    // 双向存储
                    String key1 = USER_SIMILARITY_KEY + ":" + user1;
                    String key2 = USER_SIMILARITY_KEY + ":" + user2;
                    redisTemplate.opsForZSet().add(key1, user2.toString(), similarity);
                    redisTemplate.opsForZSet().add(key2, user1.toString(), similarity);
                }
            }
        }

        log.info("用户相似度矩阵计算完成，涉及 {} 个用户", userList.size());
    }

    private double cosineSimilarity(Map<String, Double> map1, Map<String, Double> map2) {
        if (map1.isEmpty() || map2.isEmpty()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(map1.keySet());
        allKeys.addAll(map2.keySet());

        for (String key : allKeys) {
            double v1 = map1.getOrDefault(key, 0.0);
            double v2 = map2.getOrDefault(key, 0.0);
            dotProduct += v1 * v2;
            norm1 += v1 * v1;
            norm2 += v2 * v2;
        }

        if (norm1 == 0 || norm2 == 0) {
            return 0.0;
        }
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
}
