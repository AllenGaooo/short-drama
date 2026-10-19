package com.bunny.flix.video.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bunny.flix.video.entity.Video;
import com.bunny.flix.video.mapper.VideoMapper;
import com.bunny.flix.video.service.TranscodeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class TranscodeServiceImpl extends ServiceImpl<VideoMapper, Video> implements TranscodeService {

    @Override
    public void transcodeVideo(Long videoId) {
        log.info("开始转码视频，videoId={}", videoId);
        Video video = this.getById(videoId);
        if (video == null) {
            log.warn("视频不存在，videoId={}", videoId);
            return;
        }

        // 更新为转码中
        updateTranscodeStatus(videoId, 1, null);

        try {
            // 模拟转码过程
            log.info("模拟转码中...videoId={}", videoId);
            Thread.sleep(3000);

            // 生成模拟HLS地址
            String hlsUrl = video.getOriginalUrl().replace(".mp4", "/hls/index.m3u8");
            updateTranscodeStatus(videoId, 2, hlsUrl);
            log.info("视频转码完成，videoId={}, hlsUrl={}", videoId, hlsUrl);
        } catch (Exception e) {
            log.error("视频转码失败，videoId={}", videoId, e);
            updateTranscodeStatus(videoId, 3, null);
        }
    }

    @Override
    public void updateTranscodeStatus(Long videoId, Integer status, String hlsUrl) {
        Video video = this.getById(videoId);
        if (video != null) {
            video.setStatus(status);
            if (hlsUrl != null) {
                video.setHlsUrl(hlsUrl);
            }
            video.setUpdateTime(LocalDateTime.now());
            this.updateById(video);
        }
    }
}
