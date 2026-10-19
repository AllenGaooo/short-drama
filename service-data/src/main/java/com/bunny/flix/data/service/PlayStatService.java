package com.bunny.flix.data.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bunny.flix.data.entity.PlayStat;
import com.bunny.flix.data.mapper.PlayStatMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayStatService {

    private final PlayStatMapper playStatMapper;

    public void incrementPlayCount(Long dramaId) {
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        LambdaQueryWrapper<PlayStat> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlayStat::getDramaId, dramaId)
               .eq(PlayStat::getType, "day")
               .eq(PlayStat::getStatDate, today);
        PlayStat stat = playStatMapper.selectOne(wrapper);

        if (stat != null) {
            stat.setCount(stat.getCount() + 1);
            playStatMapper.updateById(stat);
        } else {
            stat = new PlayStat();
            stat.setDramaId(dramaId);
            stat.setType("day");
            stat.setStatDate(today);
            stat.setCount(1L);
            playStatMapper.insert(stat);
        }
    }

    public Long getPlayCount(Long dramaId, String type) {
        LambdaQueryWrapper<PlayStat> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlayStat::getDramaId, dramaId)
               .eq(PlayStat::getType, type);
        return playStatMapper.selectList(wrapper).stream()
                .mapToLong(PlayStat::getCount)
                .sum();
    }
}
