package com.bunny.flix.drama.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bunny.flix.drama.entity.Tag;
import com.bunny.flix.drama.mapper.TagMapper;
import com.bunny.flix.drama.service.TagService;
import com.bunny.flix.feign.idgenerator.BizTypeEnum;
import com.bunny.flix.feign.idgenerator.IdGeneratorFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements TagService {

    @Autowired
    private IdGeneratorFeignClient idGeneratorFeignClient;

    @Override
    public List<Tag> listTags(Long parentId) {
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(parentId != null, Tag::getParentId, parentId);
        wrapper.orderByAsc(Tag::getSortOrder);
        return this.list(wrapper);
    }

    @Override
    public List<Tag> listAllTags() {
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Tag::getSortOrder);
        return this.list(wrapper);
    }

    @Override
    public Tag getTag(Long id) {
        return this.getById(id);
    }

    @Override
    public Tag createTag(Tag tag) {
        if (tag.getId() == null) {
            tag.setId(idGeneratorFeignClient.nextId(BizTypeEnum.TAG).getData());
        }
        tag.setCreateTime(LocalDateTime.now());
        this.save(tag);
        return tag;
    }

    @Override
    public Tag updateTag(Tag tag) {
        this.updateById(tag);
        return this.getById(tag.getId());
    }

    @Override
    public void deleteTag(Long id) {
        this.removeById(id);
    }
}
