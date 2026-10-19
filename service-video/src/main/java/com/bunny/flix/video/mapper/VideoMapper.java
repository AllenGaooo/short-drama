package com.bunny.flix.video.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bunny.flix.video.entity.Video;
import org.apache.ibatis.annotations.Mapper;

/**
 * 视频数据访问层
 */
@Mapper
public interface VideoMapper extends BaseMapper<Video> {
}
