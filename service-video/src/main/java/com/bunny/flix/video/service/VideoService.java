package com.bunny.flix.video.service;

import com.bunny.flix.video.entity.Video;

public interface VideoService {

    Video uploadVideo(Long episodeId, String fileUrl);

    String getVideoUrl(Long episodeId, Long userId);

    String getHlsUrl(Long episodeId, String resolution);

    Video getVideoInfo(Long videoId);
}
