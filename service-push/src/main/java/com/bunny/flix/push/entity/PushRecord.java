package com.bunny.flix.push.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 推送记录实体类，对应数据库 t_push_record 表
 * 记录每次App推送的详情和结果
 */
@Data
@TableName("t_push_record")
public class PushRecord {
    @TableId private Long id;
    /** 目标用户ID */
    private Long userId;
    /** 推送标题 */
    private String title;
    /** 推送内容 */
    private String content;
    /** 推送方式：app_push-应用推送，sms-短信 */
    private String pushType;
    /** 推送时间 */
    private LocalDateTime pushTime;
    /** 推送结果：success/fail */
    private String result;
    private LocalDateTime createTime;
}
