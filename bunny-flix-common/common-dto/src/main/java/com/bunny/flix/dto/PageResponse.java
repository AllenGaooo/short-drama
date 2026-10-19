package com.bunny.flix.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * 分页响应结果
 * 返回给前端的分页数据，包含数据列表和分页信息
 *
 * @param <T> 列表中的数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    /** 当前页的数据列表 */
    private List<T> records = Collections.emptyList();
    /** 总记录数 */
    private Long total = 0L;
    /** 当前页码 */
    private Integer page = 1;
    /** 每页条数 */
    private Integer size = 10;

    /** 快速构建分页响应 */
    public static <T> PageResponse<T> of(List<T> records, Long total, Integer page, Integer size) {
        return new PageResponse<>(records, total, page, size);
    }

    /** 计算总页数，比如总共25条，每页10条，总页数就是3页 */
    public Long getTotalPages() {
        return size > 0 ? (total + size - 1) / size : 0;
    }
}
