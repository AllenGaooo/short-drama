package com.bunny.flix.recommend.service.impl;

import cn.hutool.json.JSONUtil;
import com.bunny.flix.recommend.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final StringRedisTemplate redisTemplate;

    private static final String USER_PROFILE_KEY = "user:profile:";
    private static final String USER_TAG_KEY = "user:tags:";
    private static final String TAG_DRAMA_KEY = "recommend:tag:";

    @Override
    public void updateUserProfile(Long userId, Long dramaId, Long watchDuration) {
        // 更新用户观看时长
        String profileKey = USER_PROFILE_KEY + userId;
        String watchKey = profileKey + ":watch:" + dramaId;
        String currentWatch = redisTemplate.opsForValue().get(watchKey);
        long totalWatch = (currentWatch != null ? Long.parseLong(currentWatch) : 0) + watchDuration;
        redisTemplate.opsForValue().set(watchKey, String.valueOf(totalWatch), 30, TimeUnit.DAYS);

        // 从短剧标签集合获取标签并更新用户偏好
        String dramaTagsKey = "drama:tags:" + dramaId;
        Map<Object, Object> dramaTags = redisTemplate.opsForHash().entries(dramaTagsKey);

        if (!dramaTags.isEmpty()) {
            String userTagsKey = USER_TAG_KEY + userId;
            for (Map.Entry<Object, Object> entry : dramaTags.entrySet()) {
                String tag = entry.getKey().toString();
                double weight = Double.parseDouble(entry.getValue().toString());

                // 观看时长越长，权重越高
                double adjustedWeight = weight * Math.min(watchDuration / 60.0, 2.0);

                redisTemplate.opsForHash().increment(userTagsKey, tag, adjustedWeight);
            }
            redisTemplate.expire(userTagsKey, 30, TimeUnit.DAYS);
        }

        // 将短剧加入标签索引
        if (!dramaTags.isEmpty()) {
            for (Object tagObj : dramaTags.keySet()) {
                String tag = tagObj.toString();
                String tagDramaKey = TAG_DRAMA_KEY + tag;
                redisTemplate.opsForSet().add(tagDramaKey, dramaId.toString());
                redisTemplate.expire(tagDramaKey, 7, TimeUnit.DAYS);
            }
        }

        log.debug("用户画像更新: userId={}, dramaId={}, watchDuration={}", userId, dramaId, watchDuration);
    }

    @Override
    public Map<String, Double> getUserTagPreferences(Long userId) {
        String userTagsKey = USER_TAG_KEY + userId;
        Map<Object, Object> rawTags = redisTemplate.opsForHash().entries(userTagsKey);

        Map<String, Double> tagPreferences = new HashMap<>();
        double totalWeight = 0.0;

        for (Map.Entry<Object, Object> entry : rawTags.entrySet()) {
            String tag = entry.getKey().toString();
            double weight = Double.parseDouble(entry.getValue().toString());
            tagPreferences.put(tag, weight);
            totalWeight += weight;
        }

        // 归一化
        if (totalWeight > 0) {
            for (Map.Entry<String, Double> entry : tagPreferences.entrySet()) {
                entry.setValue(entry.getValue() / totalWeight);
            }
        }

        return tagPreferences;
    }
}
