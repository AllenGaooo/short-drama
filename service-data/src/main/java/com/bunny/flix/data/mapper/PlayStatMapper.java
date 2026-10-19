package com.bunny.flix.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bunny.flix.data.entity.PlayStat;
import org.apache.ibatis.annotations.Mapper;

/**
 * 播放统计数据访问层
 */
@Mapper
public interface PlayStatMapper extends BaseMapper<PlayStat> {
}
