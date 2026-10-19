package com.bunny.flix.video.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bunny.flix.video.entity.Video;
import com.bunny.flix.video.mapper.VideoMapper;
import com.bunny.flix.video.service.VideoService;
import com.bunny.flix.feign.idgenerator.BizTypeEnum;
import com.bunny.flix.feign.idgenerator.IdGeneratorFeignClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoServiceImpl extends ServiceImpl<VideoMapper, Video> implements VideoService {

    private final RocketMQTemplate rocketMQTemplate;
    private final IdGeneratorFeignClient idGeneratorFeignClient;

    @Override
    public Video uploadVideo(Long episodeId, String fileUrl) {
        Video video = new Video();
        video.setId(idGeneratorFeignClient.nextId(BizTypeEnum.VIDEO).getData());
        video.setEpisodeId(episodeId);
        video.setOriginalUrl(fileUrl);
        video.setStatus(0); // 待转码
        video.setCreateTime(LocalDateTime.now());
        video.setUpdateTime(LocalDateTime.now());
        this.save(video);

        // 发送转码消息
        rocketMQTemplate.convertAndSend("video-transcode", video.getId());
        log.info("视频上传成功，videoId={}, 已发送转码消息", video.getId());

        return video;
    }

    @Override
    public String getVideoUrl(Long episodeId, Long userId) {
        LambdaQueryWrapper<Video> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Video::getEpisodeId, episodeId);
        wrapper.eq(Video::getStatus, 2); // 转码完成
        wrapper.orderByDesc(Video::getCreateTime);
        Video video = this.getOne(wrapper);

        if (video == null) {
            return null;
        }
        // 优先返回HLS地址，否则返回原始地址
        return video.getHlsUrl() != null ? video.getHlsUrl() : video.getOriginalUrl();
    }

    @Override
    public String getHlsUrl(Long episodeId, String resolution) {
        LambdaQueryWrapper<Video> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Video::getEpisodeId, episodeId);
        wrapper.eq(Video::getStatus, 2);
        wrapper.orderByDesc(Video::getCreateTime);
        Video video = this.getOne(wrapper);

        if (video == null || video.getHlsUrl() == null) {
            return null;
        }
        // 返回HLS播放地址（实际应用中会根据resolution返回不同清晰度）
        return video.getHlsUrl();
    }

    @Override
    public Video getVideoInfo(Long videoId) {
        return this.getById(videoId);
    }
}
