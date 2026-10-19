package com.bunny.flix.video.mq;

import com.bunny.flix.video.service.TranscodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(topic = "video-transcode", consumerGroup = "video-transcode-group")
public class TranscodeConsumer implements RocketMQListener<Long> {

    private final TranscodeService transcodeService;

    @Override
    public void onMessage(Long videoId) {
        log.info("收到转码消息，videoId={}", videoId);
        try {
            transcodeService.transcodeVideo(videoId);
        } catch (Exception e) {
            log.error("处理转码消息失败，videoId={}", videoId, e);
        }
    }
}
