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
 * 发行方实体类，对应数据库 t_publisher 表
 * 每个短剧都有一个发行方（制作公司）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_publisher")
public class Publisher implements Serializable {

    @TableId(type = IdType.INPUT)
    private Long id;

    /** 发行方名称 */
    private String name;

    /** Logo URL */
    private String logo;

    /** 简介 */
    private String description;

    /** 联系方式 */
    private String contact;

    /** 资质信息 */
    private String qualification;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
