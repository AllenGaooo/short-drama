package com.bunny.flix.search.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bunny.flix.search.entity.SearchHistory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 搜索历史数据访问层
 */
@Mapper
public interface SearchHistoryMapper extends BaseMapper<SearchHistory> {
}
