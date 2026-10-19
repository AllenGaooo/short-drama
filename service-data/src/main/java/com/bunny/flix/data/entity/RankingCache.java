package com.bunny.flix.data.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 排行榜缓存实体（非数据库表）
 * 存储计算好的热门短剧排行，缓存在 Redis 中
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RankingCache {

    /** 短剧ID */
    private Long dramaId;
    /** 短剧标题 */
    private String title;
    /** 封面URL */
    private String cover;
    /** 热度分数 */
    private Double heatScore;
    /** 排名 */
    private Integer rank;
}
