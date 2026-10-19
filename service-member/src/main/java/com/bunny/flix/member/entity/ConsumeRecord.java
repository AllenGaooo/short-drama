package com.bunny.flix.member.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消费记录实体类，对应数据库 t_consume_record 表
 * 记录用户金币消费明细（购买剧集、VIP等）
 */
@Data
@TableName("t_consume_record")
public class ConsumeRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    /** 用户ID */
    private Long userId;
    /** 订单号 */
    private String orderNo;
    /** 消费金额（金币） */
    private Long amount;
    /** 消费类型：1-购买剧集，2-购买VIP，3-打赏 */
    private Integer type;
    /** 短剧ID */
    private Long dramaId;
    /** 消费描述 */
    private String description;
    private LocalDateTime createTime;
}
