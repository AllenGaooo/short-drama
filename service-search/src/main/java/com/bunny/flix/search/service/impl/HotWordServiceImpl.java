package com.bunny.flix.search.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bunny.flix.core.exception.BusinessException;
import com.bunny.flix.search.entity.HotWord;
import com.bunny.flix.search.mapper.HotWordMapper;
import com.bunny.flix.search.service.HotWordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HotWordServiceImpl implements HotWordService {

    private final HotWordMapper hotWordMapper;

    @Override
    public List<HotWord> listHotWords() {
        LambdaQueryWrapper<HotWord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HotWord::getEnabled, 1)
                .orderByAsc(HotWord::getSortOrder)
                .orderByDesc(HotWord::getCreateTime);
        return hotWordMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public void addHotWord(HotWord hotWord) {
        hotWord.setCreateTime(LocalDateTime.now());
        hotWord.setUpdateTime(LocalDateTime.now());
        if (hotWord.getEnabled() == null) {
            hotWord.setEnabled(1);
        }
        if (hotWord.getSortOrder() == null) {
            hotWord.setSortOrder(0);
        }
        hotWordMapper.insert(hotWord);
        log.info("新增热搜词: word={}", hotWord.getWord());
    }

    @Override
    @Transactional
    public void updateHotWord(HotWord hotWord) {
        HotWord existing = hotWordMapper.selectById(hotWord.getId());
        if (existing == null) {
            throw new BusinessException(404, "热搜词不存在");
        }
        hotWord.setUpdateTime(LocalDateTime.now());
        hotWordMapper.updateById(hotWord);
        log.info("更新热搜词: id={}", hotWord.getId());
    }

    @Override
    @Transactional
    public void deleteHotWord(Long id) {
        HotWord existing = hotWordMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(404, "热搜词不存在");
        }
        hotWordMapper.deleteById(id);
        log.info("删除热搜词: id={}", id);
    }
}
