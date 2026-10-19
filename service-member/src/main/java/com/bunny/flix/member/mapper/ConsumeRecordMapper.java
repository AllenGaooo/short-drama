package com.bunny.flix.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bunny.flix.member.entity.ConsumeRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 消费记录数据访问层
 */
@Mapper
public interface ConsumeRecordMapper extends BaseMapper<ConsumeRecord> {
}
