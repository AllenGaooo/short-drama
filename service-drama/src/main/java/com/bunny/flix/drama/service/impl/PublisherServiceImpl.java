package com.bunny.flix.drama.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bunny.flix.drama.entity.Publisher;
import com.bunny.flix.drama.mapper.PublisherMapper;
import com.bunny.flix.drama.service.PublisherService;
import com.bunny.flix.feign.idgenerator.BizTypeEnum;
import com.bunny.flix.feign.idgenerator.IdGeneratorFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class PublisherServiceImpl extends ServiceImpl<PublisherMapper, Publisher> implements PublisherService {

    @Autowired
    private IdGeneratorFeignClient idGeneratorFeignClient;

    @Override
    public Page<Publisher> listPublishers(Integer page, Integer pageSize, String keyword) {
        LambdaQueryWrapper<Publisher> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(keyword), Publisher::getName, keyword);
        wrapper.orderByDesc(Publisher::getCreateTime);

        Page<Publisher> pageParam = new Page<>(page != null ? page : 1, pageSize != null ? pageSize : 10);
        return this.page(pageParam, wrapper);
    }

    @Override
    public Publisher getPublisher(Long id) {
        return this.getById(id);
    }

    @Override
    public Publisher createPublisher(Publisher publisher) {
        if (publisher.getId() == null) {
            publisher.setId(idGeneratorFeignClient.nextId(BizTypeEnum.PUBLISHER).getData());
        }
        publisher.setCreateTime(LocalDateTime.now());
        publisher.setUpdateTime(LocalDateTime.now());
        this.save(publisher);
        return publisher;
    }

    @Override
    public Publisher updatePublisher(Publisher publisher) {
        publisher.setUpdateTime(LocalDateTime.now());
        this.updateById(publisher);
        return this.getById(publisher.getId());
    }

    @Override
    public void deletePublisher(Long id) {
        this.removeById(id);
    }
}
