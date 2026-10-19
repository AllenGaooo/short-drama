package com.bunny.flix.recommend.service.impl;

import cn.hutool.json.JSONUtil;
import com.bunny.flix.recommend.service.RecommendService;
import com.bunny.flix.recommend.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendServiceImpl implements RecommendService {

    private final StringRedisTemplate redisTemplate;
    private final UserProfileService userProfileService;

    private static final String RECOMMEND_KEY_PREFIX = "recommend:user:";
    private static final String HOT_RECOMMEND_KEY = "recommend:hot";
    private static final String RECOMMEND_EXPIRE_HOURS = "24";

    @Override
    public List<Map<String, Object>> getRecommendations(Long userId, Integer page, Integer size) {
        String key = RECOMMEND_KEY_PREFIX + userId;
        String cached = redisTemplate.opsForValue().get(key);

        if (cached != null && !cached.isEmpty()) {
            List<Map<String, Object>> allRecommendations = JSONUtil.toList(cached,
                    new cn.hutool.core.lang.TypeReference<Map<String, Object>>() {}.getType());

            int start = (page - 1) * size;
            int end = Math.min(start + size, allRecommendations.size());
            if (start >= allRecommendations.size()) {
                return Collections.emptyList();
            }
            return allRecommendations.subList(start, end);
        }

        // 缓存未命中，实时计算
        log.info("推荐缓存未命中，实时计算: userId={}", userId);
        List<Map<String, Object>> recommendations = calculateContentBased(userId);
        cacheRecommendations(userId, recommendations);

        int start = (page - 1) * size;
        int end = Math.min(start + size, recommendations.size());
        if (start >= recommendations.size()) {
            return Collections.emptyList();
        }
        return recommendations.subList(start, end);
    }

    @Override
    public List<Map<String, Object>> calculateCollaborativeFiltering(Long userId) {
        // 简化版协同过滤：基于用户标签偏好的相似度
        Map<String, Double> userTags = userProfileService.getUserTagPreferences(userId);
        List<Map<String, Object>> recommendations = new ArrayList<>();

        // 获取所有用户的推荐缓存作为相似度计算基础
        Set<String> keys = redisTemplate.keys(RECOMMEND_KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return recommendations;
        }

        // 简化：基于标签相似度推荐
        for (String key : keys) {
            Long otherUserId = extractUserIdFromKey(key);
            if (otherUserId == null || otherUserId.equals(userId)) {
                continue;
            }

            Map<String, Double> otherTags = userProfileService.getUserTagPreferences(otherUserId);
            double similarity = calculateSimilarity(userTags, otherTags);

            if (similarity > 0.3) {
                String cached = redisTemplate.opsForValue().get(key);
                if (cached != null) {
                    List<Map<String, Object>> otherRecs = JSONUtil.toList(cached,
                            new cn.hutool.core.lang.TypeReference<Map<String, Object>>() {}.getType());
                    for (Map<String, Object> rec : otherRecs) {
                        rec.put("similarityScore", similarity);
                        recommendations.add(rec);
                    }
                }
            }
        }

        // 按相似度排序去重
        return recommendations.stream()
                .collect(Collectors.toMap(
                        m -> m.get("dramaId"),
                        m -> m,
                        (m1, m2) -> m1
                ))
                .values()
                .stream()
                .sorted((a, b) -> {
                    Double sa = (Double) a.getOrDefault("similarityScore", 0.0);
                    Double sb = (Double) b.getOrDefault("similarityScore", 0.0);
                    return sb.compareTo(sa);
                })
                .limit(50)
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> calculateContentBased(Long userId) {
        // 基于用户标签偏好的内容推荐
        Map<String, Double> tagPreferences = userProfileService.getUserTagPreferences(userId);
        List<Map<String, Object>> recommendations = new ArrayList<>();

        if (tagPreferences.isEmpty()) {
            // 新用户：返回热门推荐
            return calculateHotRecommendations(1, 50);
        }

        // 按标签偏好排序
        List<Map.Entry<String, Double>> sortedTags = tagPreferences.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toList());

        // 从Redis获取各标签下的短剧
        int rank = 1;
        for (Map.Entry<String, Double> entry : sortedTags) {
            String tagKey = "recommend:tag:" + entry.getKey();
            Set<String> dramaIds = redisTemplate.opsForSet().members(tagKey);
            if (dramaIds != null) {
                for (String dramaId : dramaIds) {
                    Map<String, Object> rec = new HashMap<>();
                    rec.put("dramaId", Long.valueOf(dramaId));
                    rec.put("score", entry.getValue() * 100);
                    rec.put("reason", "标签匹配: " + entry.getKey());
                    rec.put("rank", rank++);
                    recommendations.add(rec);
                }
            }
        }

        return recommendations.stream()
                .sorted((a, b) -> {
                    Double sa = (Double) a.getOrDefault("score", 0.0);
                    Double sb = (Double) b.getOrDefault("score", 0.0);
                    return sb.compareTo(sa);
                })
                .limit(50)
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> calculateHotRecommendations(Integer page, Integer size) {
        String cached = redisTemplate.opsForValue().get(HOT_RECOMMEND_KEY);
        if (cached != null && !cached.isEmpty()) {
            List<Map<String, Object>> allHot = JSONUtil.toList(cached,
                    new cn.hutool.core.lang.TypeReference<Map<String, Object>>() {}.getType());

            int start = (page - 1) * size;
            int end = Math.min(start + size, allHot.size());
            if (start >= allHot.size()) {
                return Collections.emptyList();
            }
            return allHot.subList(start, end);
        }

        // 简化版热度计算：从Redis获取播放量、点赞数、收藏数
        List<Map<String, Object>> hotList = new ArrayList<>();

        // 尝试从Redis获取热门短剧ID
        Set<String> hotDramaIds = redisTemplate.opsForZSet()
                .reverseRange("drama:hot:score", 0, 99);

        if (hotDramaIds != null && !hotDramaIds.isEmpty()) {
            int rank = 1;
            for (String dramaId : hotDramaIds) {
                Map<String, Object> drama = new HashMap<>();
                drama.put("dramaId", Long.valueOf(dramaId));
                drama.put("rank", rank++);
                hotList.add(drama);
            }
        }

        // 缓存到Redis，2小时过期
        redisTemplate.opsForValue().set(HOT_RECOMMEND_KEY, JSONUtil.toJsonStr(hotList), 2, TimeUnit.HOURS);

        int start = (page - 1) * size;
        int end = Math.min(start + size, hotList.size());
        if (start >= hotList.size()) {
            return Collections.emptyList();
        }
        return hotList.subList(start, end);
    }

    @Override
    public void cacheRecommendations(Long userId, List<Map<String, Object>> recommendations) {
        String key = RECOMMEND_KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(recommendations),
                Long.parseLong(RECOMMEND_EXPIRE_HOURS), TimeUnit.HOURS);
        log.info("推荐结果已缓存: userId={}, count={}", userId, recommendations.size());
    }

    private Long extractUserIdFromKey(String key) {
        try {
            String idStr = key.substring(RECOMMEND_KEY_PREFIX.length());
            return Long.valueOf(idStr);
        } catch (Exception e) {
            return null;
        }
    }

    private double calculateSimilarity(Map<String, Double> tags1, Map<String, Double> tags2) {
        if (tags1.isEmpty() || tags2.isEmpty()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        Set<String> allTags = new HashSet<>();
        allTags.addAll(tags1.keySet());
        allTags.addAll(tags2.keySet());

        for (String tag : allTags) {
            double v1 = tags1.getOrDefault(tag, 0.0);
            double v2 = tags2.getOrDefault(tag, 0.0);
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
