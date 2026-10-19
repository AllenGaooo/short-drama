package com.bunny.flix.marketing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bunny.flix.marketing.entity.Task;
import org.apache.ibatis.annotations.Mapper;

/**
 * 任务数据访问层
 */
@Mapper
public interface TaskMapper extends BaseMapper<Task> {
}
