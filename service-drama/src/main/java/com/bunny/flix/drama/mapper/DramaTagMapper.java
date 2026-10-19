package com.bunny.flix.drama.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bunny.flix.drama.entity.DramaTag;
import org.apache.ibatis.annotations.Mapper;

/**
 * 短剧-标签关联数据访问层
 */
@Mapper
public interface DramaTagMapper extends BaseMapper<DramaTag> {
}
