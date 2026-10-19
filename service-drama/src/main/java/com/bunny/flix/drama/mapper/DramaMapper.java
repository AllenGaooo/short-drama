package com.bunny.flix.drama.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bunny.flix.drama.entity.Drama;
import org.apache.ibatis.annotations.Mapper;

/**
 * 短剧数据访问层
 * 继承 MyBatis-Plus 的 BaseMapper，自带增删改查方法
 */
@Mapper
public interface DramaMapper extends BaseMapper<Drama> {
}
