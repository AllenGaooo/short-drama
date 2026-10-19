package com.bunny.flix.push.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bunny.flix.push.entity.PushRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 推送记录数据访问层
 */
@Mapper
public interface PushRecordMapper extends BaseMapper<PushRecord> {
}
