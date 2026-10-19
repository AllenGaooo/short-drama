package com.bunny.flix.interaction.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bunny.flix.interaction.entity.Danmaku;
import com.bunny.flix.interaction.mapper.DanmakuMapper;
import com.bunny.flix.interaction.service.DanmakuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DanmakuServiceImpl implements DanmakuService {

    private final DanmakuMapper danmakuMapper;

    @Override
    @Transactional
    public Danmaku sendDanmaku(Long userId, Long dramaId, Long episodeId, String content, Double playTime, String color, Integer font) {
        Danmaku danmaku = new Danmaku();
        danmaku.setUserId(userId);
        danmaku.setDramaId(dramaId);
        danmaku.setEpisodeId(episodeId);
        danmaku.setContent(content);
        danmaku.setPlayTime(playTime);
        danmaku.setColor(color != null ? color : "#FFFFFF");
        danmaku.setFont(font != null ? font : 16);
        danmaku.setCreateTime(LocalDateTime.now());

        danmakuMapper.insert(danmaku);
        log.info("发送弹幕成功: userId={}, episodeId={}, playTime={}", userId, episodeId, playTime);
        return danmaku;
    }

    @Override
    public List<Danmaku> getDanmakuList(Long episodeId, Double playTime) {
        LambdaQueryWrapper<Danmaku> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Danmaku::getEpisodeId, episodeId);

        if (playTime != null) {
            // Get danmaku within a 30-second window around the play time
            wrapper.between(Danmaku::getPlayTime, playTime - 15, playTime + 15);
        }

        wrapper.orderByAsc(Danmaku::getPlayTime);
        return danmakuMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public void reportDanmaku(Long danmakuId, Long userId) {
        Danmaku danmaku = danmakuMapper.selectById(danmakuId);
        if (danmaku == null) {
            log.warn("弹幕不存在: danmakuId={}", danmakuId);
            return;
        }
        log.info("弹幕被举报: danmakuId={}, reporterUserId={}", danmakuId, userId);
        // Report logic could be extended to insert into a report table
    }

    @Override
    @Transactional
    public void deleteDanmaku(Long danmakuId, Long userId) {
        Danmaku danmaku = danmakuMapper.selectById(danmakuId);
        if (danmaku == null) {
            log.warn("弹幕不存在: danmakuId={}", danmakuId);
            return;
        }
        danmakuMapper.deleteById(danmakuId);
        log.info("删除弹幕成功: danmakuId={}, operatorUserId={}", danmakuId, userId);
    }
}
