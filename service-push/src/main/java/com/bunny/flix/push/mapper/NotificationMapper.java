package com.bunny.flix.push.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bunny.flix.push.entity.Notification;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通知数据访问层
 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
}
