package com.bunny.flix.interaction.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bunny.flix.interaction.entity.Share;
import com.bunny.flix.interaction.mapper.ShareMapper;
import com.bunny.flix.interaction.service.ShareService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShareServiceImpl implements ShareService {

    private final ShareMapper shareMapper;

    @Override
    @Transactional
    public void recordShare(Long userId, Long dramaId, String platform) {
        Share share = new Share();
        share.setUserId(userId);
        share.setDramaId(dramaId);
        share.setPlatform(platform);
        share.setCreateTime(LocalDateTime.now());

        shareMapper.insert(share);
        log.info("记录分享成功: userId={}, dramaId={}, platform={}", userId, dramaId, platform);
    }

    @Override
    public Map<String, Long> getShareStats(Long dramaId) {
        LambdaQueryWrapper<Share> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Share::getDramaId, dramaId);
        List<Share> shares = shareMapper.selectList(wrapper);

        Map<String, Long> stats = new HashMap<>();
        stats.put("total", (long) shares.size());

        Map<String, Long> platformStats = shares.stream()
                .collect(Collectors.groupingBy(Share::getPlatform, Collectors.counting()));
        stats.putAll(platformStats);

        return stats;
    }
}
