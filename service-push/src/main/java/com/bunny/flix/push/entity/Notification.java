package com.bunny.flix.push.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 通知实体类，对应数据库 t_notification 表
 * 存储发送给用户的站内通知（如被关注、剧集更新等）
 */
@Data
@TableName("t_notification")
public class Notification {
    @TableId private Long id;
    /** 接收用户ID */
    private Long userId;
    /** 通知标题 */
    private String title;
    /** 通知内容 */
    private String content;
    /** 通知类型：system-系统，interaction-互动，update-更新 */
    private String type;
    /** 是否已读：0-未读，1-已读 */
    private Integer isRead;
    private LocalDateTime createTime;
}
