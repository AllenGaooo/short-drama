package com.bunny.flix.recommend.service;

import java.util.Map;

public interface UserProfileService {

    void updateUserProfile(Long userId, Long dramaId, Long watchDuration);

    Map<String, Double> getUserTagPreferences(Long userId);
}
