package com.bunny.flix.data.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bunny.flix.data.entity.PlayStat;
import com.bunny.flix.data.mapper.PlayStatMapper;
import com.bunny.flix.data.service.PlayStatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayStatServiceImpl implements PlayStatService {

    private final PlayStatMapper playStatMapper;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public void incrementPlayCount(Long dramaId) {
        String today = LocalDate.now().format(DTF);
        String[] types = {"day", "week", "month", "total"};

        for (String type : types) {
            LambdaQueryWrapper<PlayStat> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(PlayStat::getDramaId, dramaId)
                   .eq(PlayStat::getType, type);

            if (!"total".equals(type)) {
                wrapper.eq(PlayStat::getStatDate, today);
            }

            PlayStat stat = playStatMapper.selectOne(wrapper);
            if (stat == null) {
                stat = new PlayStat();
                stat.setDramaId(dramaId);
                stat.setType(type);
                stat.setStatDate("total".equals(type) ? null : today);
                stat.setCount(1L);
                playStatMapper.insert(stat);
            } else {
                stat.setCount(stat.getCount() + 1);
                playStatMapper.updateById(stat);
            }
        }
    }

    @Override
    public List<PlayStat> getPlayStats(Long dramaId, String type) {
        LambdaQueryWrapper<PlayStat> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlayStat::getDramaId, dramaId);
        if (type != null && !type.isEmpty()) {
            wrapper.eq(PlayStat::getType, type);
        }
        wrapper.orderByDesc(PlayStat::getStatDate);
        return playStatMapper.selectList(wrapper);
    }

    @Override
    public Map<String, Object> getDramaPlayRank(Integer page, Integer size) {
        Page<PlayStat> pageParam = new Page<>(page, size);

        LambdaQueryWrapper<PlayStat> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlayStat::getType, "total")
               .orderByDesc(PlayStat::getCount);

        Page<PlayStat> result = playStatMapper.selectPage(pageParam, wrapper);

        Map<String, Object> map = new HashMap<>();
        map.put("records", result.getRecords());
        map.put("total", result.getTotal());
        map.put("page", page);
        map.put("size", size);
        return map;
    }
}
