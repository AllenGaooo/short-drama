package com.bunny.flix.data.service.impl;

import cn.hutool.json.JSONUtil;
import com.bunny.flix.data.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final StringRedisTemplate redisTemplate;

    private static final String USER_PROFILE_PREFIX = "user:profile:";

    @Override
    public void updateUserProfile(Long userId, Long dramaId, Long watchDuration) {
        String key = USER_PROFILE_PREFIX + userId;
        String dramaKey = "drama:" + dramaId;
        String field = "watch_duration";

        // Increment watch duration for this drama
        redisTemplate.opsForHash().increment(key, dramaKey + ":" + field, watchDuration);
        redisTemplate.expire(key, 30, TimeUnit.DAYS);
        log.info("Updated user profile: userId={}, dramaId={}, duration={}", userId, dramaId, watchDuration);
    }

    @Override
    public Map<String, Object> getUserProfile(Long userId) {
        String key = USER_PROFILE_PREFIX + userId;
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);

        Map<String, Object> profile = new HashMap<>();
        profile.put("userId", userId);
        profile.put("preferences", entries);

        // Parse watched dramas
        List<Map<String, Object>> watchedDramas = new ArrayList<>();
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            String hashKey = entry.getKey().toString();
            if (hashKey.startsWith("drama:")) {
                String dramaIdStr = hashKey.substring(6).split(":")[0];
                Map<String, Object> drama = new HashMap<>();
                drama.put("dramaId", Long.valueOf(dramaIdStr));
                drama.put("watchDuration", Long.valueOf(entry.getValue().toString()));
                watchedDramas.add(drama);
            }
        }
        profile.put("watchedDramas", watchedDramas);
        return profile;
    }
}
