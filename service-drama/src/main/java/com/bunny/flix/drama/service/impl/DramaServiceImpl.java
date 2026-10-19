package com.bunny.flix.drama.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bunny.flix.drama.entity.Drama;
import com.bunny.flix.drama.entity.DramaTag;
import com.bunny.flix.drama.mapper.DramaMapper;
import com.bunny.flix.drama.mapper.DramaTagMapper;
import com.bunny.flix.drama.mq.DramaSyncProducer;
import com.bunny.flix.drama.service.DramaService;
import com.bunny.flix.feign.drama.dto.DramaDTO;
import com.bunny.flix.feign.idgenerator.BizTypeEnum;
import com.bunny.flix.feign.idgenerator.IdGeneratorFeignClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DramaServiceImpl extends ServiceImpl<DramaMapper, Drama> implements DramaService {

    private final DramaSyncProducer dramaSyncProducer;
    private final IdGeneratorFeignClient idGeneratorFeignClient;
    private final DramaTagMapper dramaTagMapper;

    @Override
    public Page<Drama> listDramas(Integer page, Integer pageSize, String keyword, Integer status, Integer payType, Long tagId) {
        LambdaQueryWrapper<Drama> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(status != null, Drama::getStatus, status);
        wrapper.eq(payType != null, Drama::getPayType, payType);
        wrapper.like(StrUtil.isNotBlank(keyword), Drama::getTitle, keyword);

        // 按标签筛选：先查关联表 t_drama_tag 得到该标签下的短剧ID，再作为 in 条件
        // （原实现接收了 tagId 参数但未使用，分类筛选因此无效）
        if (tagId != null) {
            List<Long> dramaIds = dramaTagMapper.selectList(
                            new LambdaQueryWrapper<DramaTag>().eq(DramaTag::getTagId, tagId))
                    .stream().map(DramaTag::getDramaId).distinct().toList();
            if (dramaIds.isEmpty()) {
                // 该标签下没有短剧，直接返回空分页，避免 in() 空集合导致 SQL 错误
                Page<Drama> empty = new Page<>(page != null ? page : 1, pageSize != null ? pageSize : 10);
                empty.setRecords(List.of());
                empty.setTotal(0);
                return empty;
            }
            wrapper.in(Drama::getId, dramaIds);
        }

        wrapper.orderByDesc(Drama::getCreateTime);

        Page<Drama> pageParam = new Page<>(page != null ? page : 1, pageSize != null ? pageSize : 10);
        return this.page(pageParam, wrapper);
    }

    @Override
    public List<Drama> listRanking(Integer size, String sortBy) {
        LambdaQueryWrapper<Drama> wrapper = new LambdaQueryWrapper<>();
        // 榜单只展示已上架内容
        wrapper.eq(Drama::getStatus, 1);

        // hot=按播放量，newest=按最新，默认按播放量
        if ("newest".equalsIgnoreCase(sortBy)) {
            wrapper.orderByDesc(Drama::getCreateTime);
        } else {
            wrapper.orderByDesc(Drama::getViews);
        }

        int limit = (size == null || size <= 0) ? 10 : Math.min(size, 100);
        wrapper.last("LIMIT " + limit);
        return this.list(wrapper);
    }

    @Override
    public Drama getDramaById(Long id) {
        Drama drama = this.getById(id);
        if (drama != null) {
            drama.setViews(drama.getViews() != null ? drama.getViews() + 1 : 1);
            this.updateById(drama);
        }
        return drama;
    }

    @Override
    public Drama createDrama(Drama drama) {
        if (drama.getId() == null) {
            drama.setId(idGeneratorFeignClient.nextId(BizTypeEnum.DRAMA).getData());
        }
        drama.setCreateTime(LocalDateTime.now());
        drama.setUpdateTime(LocalDateTime.now());
        drama.setViews(0L);
        drama.setLikes(0L);
        drama.setCollects(0L);
        this.save(drama);
        // 同步短剧到 ES
        dramaSyncProducer.sendDramaSync(toDramaDTO(drama));
        return drama;
    }

    @Override
    public Drama updateDrama(Drama drama) {
        drama.setUpdateTime(LocalDateTime.now());
        this.updateById(drama);
        Drama updated = this.getById(drama.getId());
        // 同步更新到 ES
        dramaSyncProducer.sendDramaSync(toDramaDTO(updated));
        return updated;
    }

    @Override
    public void deleteDrama(Long id) {
        this.removeById(id);
        // 删除时也同步（消费者处理删除）
        DramaDTO dto = new DramaDTO();
        dto.setId(id);
        dramaSyncProducer.sendDramaSync(dto);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        Drama drama = this.getById(id);
        if (drama != null) {
            drama.setStatus(status);
            drama.setUpdateTime(LocalDateTime.now());
            this.updateById(drama);
        }
    }

    @Override
    public void updateCompleted(Long id, Integer completed) {
        Drama drama = this.getById(id);
        if (drama != null) {
            drama.setCompleted(completed);
            drama.setUpdateTime(LocalDateTime.now());
            this.updateById(drama);
        }
    }

    private DramaDTO toDramaDTO(Drama drama) {
        DramaDTO dto = new DramaDTO();
        dto.setId(drama.getId());
        dto.setTitle(drama.getTitle());
        dto.setCover(drama.getCover());
        dto.setDescription(drama.getDescription());
        dto.setStatus(drama.getStatus());
        dto.setCompleted(drama.getCompleted());
        dto.setPayType(drama.getPayType());
        dto.setPrice(drama.getPrice());
        dto.setTotalEpisodes(drama.getTotalEpisodes());
        dto.setPublisherId(drama.getPublisherId());
        dto.setViews(drama.getViews());
        dto.setLikes(drama.getLikes());
        dto.setCollects(drama.getCollects());
        dto.setCreateTime(drama.getCreateTime());
        dto.setUpdateTime(drama.getUpdateTime());
        return dto;
    }
}
