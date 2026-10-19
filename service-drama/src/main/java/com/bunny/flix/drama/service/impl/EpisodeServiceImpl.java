package com.bunny.flix.drama.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bunny.flix.drama.entity.Episode;
import com.bunny.flix.drama.mapper.EpisodeMapper;
import com.bunny.flix.drama.service.EpisodeService;
import com.bunny.flix.feign.idgenerator.BizTypeEnum;
import com.bunny.flix.feign.idgenerator.IdGeneratorFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EpisodeServiceImpl extends ServiceImpl<EpisodeMapper, Episode> implements EpisodeService {

    @Autowired
    private IdGeneratorFeignClient idGeneratorFeignClient;

    @Override
    public List<Episode> listEpisodes(Long dramaId) {
        LambdaQueryWrapper<Episode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Episode::getDramaId, dramaId);
        wrapper.orderByAsc(Episode::getSortOrder);
        return this.list(wrapper);
    }

    @Override
    public Episode getEpisode(Long id) {
        return this.getById(id);
    }

    @Override
    public Episode createEpisode(Episode episode) {
        if (episode.getId() == null) {
            episode.setId(idGeneratorFeignClient.nextId(BizTypeEnum.EPISODE).getData());
        }
        episode.setCreateTime(LocalDateTime.now());
        episode.setUpdateTime(LocalDateTime.now());
        this.save(episode);
        return episode;
    }

    @Override
    public Episode updateEpisode(Episode episode) {
        episode.setUpdateTime(LocalDateTime.now());
        this.updateById(episode);
        return this.getById(episode.getId());
    }

    @Override
    public void deleteEpisode(Long id) {
        this.removeById(id);
    }

    @Override
    public void updateSort(Long episodeId, Integer sortOrder) {
        Episode episode = this.getById(episodeId);
        if (episode != null) {
            episode.setSortOrder(sortOrder);
            episode.setUpdateTime(LocalDateTime.now());
            this.updateById(episode);
        }
    }

    @Override
    public void updateFreeStatus(Long episodeId, Integer free) {
        Episode episode = this.getById(episodeId);
        if (episode != null) {
            episode.setFree(free);
            episode.setUpdateTime(LocalDateTime.now());
            this.updateById(episode);
        }
    }
}
