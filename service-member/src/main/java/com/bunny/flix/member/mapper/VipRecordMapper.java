package com.bunny.flix.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bunny.flix.member.entity.VipRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * VIP记录数据访问层
 */
@Mapper
public interface VipRecordMapper extends BaseMapper<VipRecord> {
}
