package com.bunny.flix.idgenerator.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * ID号段实体类，对应数据库 t_id_segment 表
 * 实现美团 Leaf 号段模式的分布式ID生成
 * 每种业务类型（如 order、user）独立维护一个号段
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_id_segment")
public class IdSegment {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 业务类型，如 "order"、"user"、"drama" */
    private String bizType;

    /** 当前已分配的最大ID */
    private Long maxId;

    /** 每次从数据库取ID的步长，步长越大，数据库访问越少 */
    private Integer step;

    /** 乐观锁版本号，防止并发更新 */
    private Integer version;

    private LocalDateTime updateTime;
}
