package com.bunny.flix.drama.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bunny.flix.drama.entity.Publisher;
import org.apache.ibatis.annotations.Mapper;

/**
 * 发行方数据访问层
 */
@Mapper
public interface PublisherMapper extends BaseMapper<Publisher> {
}
