package com.bunny.flix.interaction.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bunny.flix.interaction.entity.Share;
import com.bunny.flix.interaction.mapper.ShareMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ShareService {

    private final ShareMapper shareMapper;

    public void recordShare(Long userId, Long dramaId, String platform) {
        Share share = new Share();
        share.setUserId(userId);
        share.setDramaId(dramaId);
        share.setPlatform(platform);
        shareMapper.insert(share);
    }

    public long getShareCount(Long dramaId) {
        LambdaQueryWrapper<Share> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Share::getDramaId, dramaId);
        return shareMapper.selectCount(wrapper);
    }
}
