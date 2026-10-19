package com.bunny.flix.drama.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bunny.flix.drama.entity.Tag;
import org.apache.ibatis.annotations.Mapper;

/**
 * 标签数据访问层
 */
@Mapper
public interface TagMapper extends BaseMapper<Tag> {
}
