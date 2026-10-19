package com.bunny.flix.data.service;

import java.util.Map;

public interface UserProfileService {

    void updateUserProfile(Long userId, Long dramaId, Long watchDuration);

    Map<String, Object> getUserProfile(Long userId);
}
