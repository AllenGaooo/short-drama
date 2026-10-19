package com.bunny.flix.drama.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bunny.flix.drama.entity.DramaActor;
import org.apache.ibatis.annotations.Mapper;

/**
 * 短剧-演员关联数据访问层
 */
@Mapper
public interface DramaActorMapper extends BaseMapper<DramaActor> {
}
