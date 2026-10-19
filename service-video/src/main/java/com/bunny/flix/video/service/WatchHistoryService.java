package com.bunny.flix.video.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bunny.flix.video.entity.WatchHistory;

public interface WatchHistoryService {

    void addHistory(Long userId, Long episodeId, Long dramaId);

    Page<WatchHistory> getHistory(Long userId, Integer page, Integer pageSize);

    void deleteHistory(Long id);
}
