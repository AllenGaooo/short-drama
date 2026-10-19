package com.bunny.flix.drama.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 短剧-演员关联实体，对应数据库 t_drama_actor 表
 * 记录短剧和演员的对应关系（多对多）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_drama_actor")
public class DramaActor implements Serializable {

    @TableId(type = IdType.INPUT)
    private Long id;

    /** 短剧ID */
    private Long dramaId;

    /** 演员ID */
    private Long actorId;

    /** 饰演的角色名 */
    private String roleName;

    /** 创建时间 */
    private LocalDateTime createTime;
}
