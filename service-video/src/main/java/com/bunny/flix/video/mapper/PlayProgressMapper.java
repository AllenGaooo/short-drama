package com.bunny.flix.video.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bunny.flix.video.entity.PlayProgress;
import org.apache.ibatis.annotations.Mapper;

/**
 * 播放进度数据访问层
 */
@Mapper
public interface PlayProgressMapper extends BaseMapper<PlayProgress> {
}
