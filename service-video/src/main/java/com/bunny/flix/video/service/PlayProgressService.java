package com.bunny.flix.video.service;

import com.bunny.flix.video.entity.PlayProgress;

public interface PlayProgressService {

    void saveProgress(Long userId, Long episodeId, Long dramaId, Double position, Double duration);

    PlayProgress getProgress(Long userId, Long episodeId);
}
