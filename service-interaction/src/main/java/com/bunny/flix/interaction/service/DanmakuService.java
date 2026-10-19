package com.bunny.flix.interaction.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bunny.flix.interaction.entity.Danmaku;
import com.bunny.flix.interaction.mapper.DanmakuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DanmakuService {

    private final DanmakuMapper danmakuMapper;

    public Danmaku sendDanmaku(Long userId, Long dramaId, Long episodeId,
                                String content, Double playTime, String color, Integer font) {
        Danmaku danmaku = new Danmaku();
        danmaku.setUserId(userId);
        danmaku.setDramaId(dramaId);
        danmaku.setEpisodeId(episodeId);
        danmaku.setContent(content);
        danmaku.setPlayTime(playTime);
        danmaku.setColor(color != null ? color : "#FFFFFF");
        danmaku.setFont(font != null ? font : 1);
        danmakuMapper.insert(danmaku);
        return danmaku;
    }

    public List<Danmaku> getDanmakuList(Long episodeId, Double playTime) {
        double range = 10.0;
        LambdaQueryWrapper<Danmaku> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Danmaku::getEpisodeId, episodeId)
               .between(Danmaku::getPlayTime, Math.max(0, playTime - range), playTime + range)
               .orderByAsc(Danmaku::getPlayTime);
        return danmakuMapper.selectList(wrapper);
    }
}
