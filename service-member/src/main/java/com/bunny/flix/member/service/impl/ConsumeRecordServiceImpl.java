package com.bunny.flix.member.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bunny.flix.member.entity.ConsumeRecord;
import com.bunny.flix.member.mapper.ConsumeRecordMapper;
import com.bunny.flix.member.service.ConsumeRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsumeRecordServiceImpl implements ConsumeRecordService {

    private final ConsumeRecordMapper consumeRecordMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordConsume(Long userId, Long amount, Long dramaId, String description) {
        String orderNo = IdUtil.getSnowflakeNextIdStr();

        ConsumeRecord record = new ConsumeRecord();
        record.setUserId(userId);
        record.setOrderNo(orderNo);
        record.setAmount(amount);
        record.setType(0);
        record.setDramaId(dramaId);
        record.setDescription(description);
        record.setCreateTime(LocalDateTime.now());
        consumeRecordMapper.insert(record);

        log.info("消费记录创建: userId={}, amount={}, dramaId={}", userId, amount, dramaId);
    }

    @Override
    public List<ConsumeRecord> getConsumeRecords(Long userId, Integer page, Integer size, Integer type) {
        Page<ConsumeRecord> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<ConsumeRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConsumeRecord::getUserId, userId);

        if (type != null) {
            wrapper.eq(ConsumeRecord::getType, type);
        }

        wrapper.orderByDesc(ConsumeRecord::getCreateTime);
        Page<ConsumeRecord> result = consumeRecordMapper.selectPage(pageParam, wrapper);
        return result.getRecords();
    }
}
