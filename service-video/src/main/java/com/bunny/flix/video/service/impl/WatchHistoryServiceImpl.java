package com.bunny.flix.video.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bunny.flix.video.entity.WatchHistory;
import com.bunny.flix.video.mapper.WatchHistoryMapper;
import com.bunny.flix.video.service.WatchHistoryService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class WatchHistoryServiceImpl extends ServiceImpl<WatchHistoryMapper, WatchHistory> implements WatchHistoryService {

    @Override
    public void addHistory(Long userId, Long episodeId, Long dramaId) {
        WatchHistory history = new WatchHistory();
        history.setUserId(userId);
        history.setEpisodeId(episodeId);
        history.setDramaId(dramaId);
        history.setWatchTime(LocalDateTime.now());
        this.save(history);
    }

    @Override
    public Page<WatchHistory> getHistory(Long userId, Integer page, Integer pageSize) {
        LambdaQueryWrapper<WatchHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WatchHistory::getUserId, userId);
        wrapper.orderByDesc(WatchHistory::getWatchTime);

        Page<WatchHistory> pageParam = new Page<>(page != null ? page : 1, pageSize != null ? pageSize : 10);
        return this.page(pageParam, wrapper);
    }

    @Override
    public void deleteHistory(Long id) {
        this.removeById(id);
    }
}
