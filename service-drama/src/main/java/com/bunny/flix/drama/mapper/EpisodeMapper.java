package com.bunny.flix.drama.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bunny.flix.drama.entity.Episode;
import org.apache.ibatis.annotations.Mapper;

/**
 * 剧集数据访问层
 */
@Mapper
public interface EpisodeMapper extends BaseMapper<Episode> {
}
