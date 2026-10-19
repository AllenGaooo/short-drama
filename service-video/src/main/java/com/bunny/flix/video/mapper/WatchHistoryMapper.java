package com.bunny.flix.video.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bunny.flix.video.entity.WatchHistory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 观看历史数据访问层
 */
@Mapper
public interface WatchHistoryMapper extends BaseMapper<WatchHistory> {
}
