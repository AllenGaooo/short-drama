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
 * 演员实体类，对应数据库 t_actor 表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_actor")
public class Actor implements Serializable {

    @TableId(type = IdType.INPUT)
    private Long id;

    /** 演员姓名 */
    private String name;

    /** 头像URL */
    private String avatar;

    /** 简介 */
    private String description;

    /** 创建时间 */
    private LocalDateTime createTime;
}
