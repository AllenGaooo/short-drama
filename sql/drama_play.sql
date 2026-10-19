-- =====================================================
-- 短剧播放微服务（drama-play-service / service-video）建表脚本
-- 包含：观看记录表 drama_watch_record
-- 执行：mysql -u root bunny_flix < sql/drama_play.sql
-- =====================================================

USE `bunny_flix`;

-- --------------- 观看记录表 ---------------
DROP TABLE IF EXISTS `drama_watch_record`;
CREATE TABLE `drama_watch_record` (
    `id`          BIGINT       NOT NULL COMMENT '主键ID',
    `user_id`     BIGINT       NOT NULL COMMENT '用户ID',
    `drama_id`    BIGINT       DEFAULT NULL COMMENT '短剧ID',
    `episode_id`  BIGINT       NOT NULL COMMENT '剧集ID',
    `watch_time`  INT          NOT NULL DEFAULT 0 COMMENT '已观看时长（秒）',
    `duration`    INT          DEFAULT NULL COMMENT '视频总时长（秒）',
    `status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '观看状态：0-观看中 1-已看完',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_episode` (`user_id`, `episode_id`),
    KEY `idx_drama_id` (`drama_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短剧观看记录表（由定时任务从Redis批量同步写入）';
