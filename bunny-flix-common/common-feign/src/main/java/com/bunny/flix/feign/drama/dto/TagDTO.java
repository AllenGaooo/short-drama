package com.bunny.flix.feign.drama.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 标签 DTO
 * 短剧的分类标签，比如"都市"、"悬疑"、"甜宠"等，支持二级分类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TagDTO implements Serializable {

    private Long id;
    /** 标签名称，比如 "都市"、"甜宠" */
    private String name;
    /** 父标签ID，0表示顶级标签 */
    private Long parentId;
    /** 层级：1-一级分类，2-二级分类 */
    private Integer level;
    /** 类型：category-分类，style-风格 */
    private String type;
    /** 排序号 */
    private Integer sortOrder;
    private LocalDateTime createTime;
}
