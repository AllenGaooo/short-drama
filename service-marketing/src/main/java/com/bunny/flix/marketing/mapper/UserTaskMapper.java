package com.bunny.flix.marketing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bunny.flix.marketing.entity.UserTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户任务数据访问层
 */
@Mapper
public interface UserTaskMapper extends BaseMapper<UserTask> {
}
