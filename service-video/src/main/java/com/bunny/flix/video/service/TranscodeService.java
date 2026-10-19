package com.bunny.flix.video.service;

public interface TranscodeService {

    void transcodeVideo(Long videoId);

    void updateTranscodeStatus(Long videoId, Integer status, String hlsUrl);
}
