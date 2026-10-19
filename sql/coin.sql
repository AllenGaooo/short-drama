-- =====================================================
-- 金币服务（service-coin）建表脚本
-- 包含：金币账户表 t_coin_account + 金币流水表 t_coin_record
-- 执行：mysql -u root bunny_flix < sql/coin.sql
-- =====================================================

USE `bunny_flix`;

-- --------------- 金币账户表 ---------------
DROP TABLE IF EXISTS `t_coin_account`;
CREATE TABLE `t_coin_account` (
    `id`            BIGINT       NOT NULL COMMENT '账户ID',
    `user_id`       BIGINT       NOT NULL COMMENT '用户ID',
    `balance`       INT          NOT NULL DEFAULT 0 COMMENT '当前余额',
    `total_earned`  INT          NOT NULL DEFAULT 0 COMMENT '累计获得',
    `total_spent`   INT          NOT NULL DEFAULT 0 COMMENT '累计消费',
    `version`       INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='金币账户表';

-- --------------- 金币流水表 ---------------
DROP TABLE IF EXISTS `t_coin_record`;
CREATE TABLE `t_coin_record` (
    `id`             BIGINT       NOT NULL COMMENT '流水ID',
    `user_id`        BIGINT       NOT NULL COMMENT '用户ID',
    `amount`         INT          NOT NULL COMMENT '变动金额（正获得/负消费）',
    `balance_after`  INT          NOT NULL DEFAULT 0 COMMENT '变动后余额',
    `biz_type`       VARCHAR(32)  NOT NULL COMMENT '业务类型：WATCH_REWARD/SIGN/CONSUME/REFUND',
    `biz_id`         VARCHAR(64)  DEFAULT '' COMMENT '业务ID（如剧集ID），用于幂等追溯',
    `remark`         VARCHAR(128) DEFAULT '' COMMENT '说明',
    `create_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    -- 关键：同一用户同一业务只产生一条流水，防止重复发币
    UNIQUE KEY `uk_user_biz` (`user_id`, `biz_type`, `biz_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='金币流水表';
