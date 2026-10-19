package com.bunny.flix.search.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bunny.flix.search.entity.HotWord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 热搜词数据访问层
 */
@Mapper
public interface HotWordMapper extends BaseMapper<HotWord> {
}
