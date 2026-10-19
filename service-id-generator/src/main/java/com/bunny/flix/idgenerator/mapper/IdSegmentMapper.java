package com.bunny.flix.idgenerator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bunny.flix.idgenerator.entity.IdSegment;
import org.apache.ibatis.annotations.Mapper;

/**
 * ID号段数据访问层
 */
@Mapper
public interface IdSegmentMapper extends BaseMapper<IdSegment> {
}
