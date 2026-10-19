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
 * 标签实体类，对应数据库 t_tag 表
 * 用于短剧分类，支持二级标签（如 "都市 > 甜宠"）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_tag")
public class Tag implements Serializable {

    @TableId(type = IdType.INPUT)
    private Long id;

    /** 标签名称，如 "都市"、"甜宠" */
    private String name;

    /** 父标签ID，0 表示一级标签 */
    private Long parentId;

    /** 层级：1-一级，2-二级 */
    private Integer level;

    /** 标签类型：drama-短剧标签，actor-演员标签，user-用户标签 */
    private String type;

    /** 排序号 */
    private Integer sortOrder;

    /** 创建时间 */
    private LocalDateTime createTime;
}
