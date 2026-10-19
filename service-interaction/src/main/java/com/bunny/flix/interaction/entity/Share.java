package com.bunny.flix.interaction.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 分享实体类，对应数据库 t_share 表
 * 记录用户将短剧分享到哪个平台
 */
@Data
@TableName("t_share")
public class Share {
    @TableId private Long id;
    /** 分享人ID */
    private Long userId;
    /** 短剧ID */
    private Long dramaId;
    /** 分享平台：wechat/weibo/qq 等 */
    private String platform;
    private LocalDateTime createTime;
}
