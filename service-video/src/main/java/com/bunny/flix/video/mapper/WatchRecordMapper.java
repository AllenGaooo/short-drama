package com.bunny.flix.video.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bunny.flix.video.entity.WatchRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 观看记录数据访问层
 */
@Mapper
public interface WatchRecordMapper extends BaseMapper<WatchRecord> {
}
