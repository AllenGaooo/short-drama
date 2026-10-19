package com.bunny.flix.search.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_hot_word")
public class HotWord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String word;

    private Integer sortOrder;

    private Integer enabled;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
