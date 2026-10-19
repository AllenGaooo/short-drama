package com.bunny.flix.video.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bunny.flix.video.entity.PlayProgress;
import com.bunny.flix.video.mapper.PlayProgressMapper;
import com.bunny.flix.video.service.PlayProgressService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class PlayProgressServiceImpl extends ServiceImpl<PlayProgressMapper, PlayProgress> implements PlayProgressService {

    @Override
    public void saveProgress(Long userId, Long episodeId, Long dramaId, Double position, Double duration) {
        LambdaQueryWrapper<PlayProgress> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlayProgress::getUserId, userId);
        wrapper.eq(PlayProgress::getEpisodeId, episodeId);
        PlayProgress progress = this.getOne(wrapper);

        if (progress == null) {
            progress = new PlayProgress();
            progress.setUserId(userId);
            progress.setEpisodeId(episodeId);
            progress.setDramaId(dramaId);
        }

        progress.setPosition(position);
        progress.setDuration(duration);
        progress.setUpdateTime(LocalDateTime.now());
        this.saveOrUpdate(progress);

        log.debug("保存播放进度：userId={}, episodeId={}, position={}", userId, episodeId, position);
    }

    @Override
    public PlayProgress getProgress(Long userId, Long episodeId) {
        LambdaQueryWrapper<PlayProgress> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlayProgress::getUserId, userId);
        wrapper.eq(PlayProgress::getEpisodeId, episodeId);
        return this.getOne(wrapper);
    }
}
