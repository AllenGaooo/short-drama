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
 * 短剧-标签关联实体，对应数据库 t_drama_tag 表
 * 记录短剧和标签的对应关系（多对多）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_drama_tag")
public class DramaTag implements Serializable {

    @TableId(type = IdType.INPUT)
    private Long id;

    /** 短剧ID */
    private Long dramaId;

    /** 标签ID */
    private Long tagId;

    /** 创建时间 */
    private LocalDateTime createTime;
}
