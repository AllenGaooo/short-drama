package com.bunny.flix.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.Min;
import java.util.List;

/**
 * 分页请求参数
 * 前端传分页信息时使用，包含页码、每页条数、排序字段等
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageRequest {

    /** 当前页码，从1开始，默认第1页 */
    @Min(value = 1, message = "页码最小为1")
    private Integer page = 1;

    /** 每页显示多少条数据，默认10条 */
    @Min(value = 1, message = "每页条数最小为1")
    private Integer size = 10;

    /** 按哪个字段排序，比如 "createTime" */
    private String sortField;
    /** 排序方向，asc=升序，desc=降序 */
    private String sortOrder;

    /**
     * 计算数据库查询的偏移量
     * 比如第2页，每页10条，偏移量就是 (2-1)*10 = 10
     */
    public long getOffset() {
        return (long) (page - 1) * size;
    }
}
