package com.bunny.flix.recommend.service;

import java.util.List;
import java.util.Map;

public interface RecommendService {

    List<Map<String, Object>> getRecommendations(Long userId, Integer page, Integer size);

    List<Map<String, Object>> calculateCollaborativeFiltering(Long userId);

    List<Map<String, Object>> calculateContentBased(Long userId);

    List<Map<String, Object>> calculateHotRecommendations(Integer page, Integer size);

    void cacheRecommendations(Long userId, List<Map<String, Object>> recommendations);
}
