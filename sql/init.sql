-- =====================================================
-- 兔兔短剧 (bunny-flix) 数据库初始化脚本
-- =====================================================

CREATE DATABASE IF NOT EXISTS `bunny_flix` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `bunny_flix`;

-- --------------- 用户服务 ---------------
DROP TABLE IF EXISTS `t_user`;
CREATE TABLE `t_user` (
    `id` BIGINT NOT NULL PRIMARY KEY COMMENT '用户ID',
    `phone` VARCHAR(20) NOT NULL COMMENT '手机号',
    `password` VARCHAR(128) DEFAULT NULL COMMENT '密码（加密）',
    `nickname` VARCHAR(64) DEFAULT '' COMMENT '昵称',
    `avatar` VARCHAR(512) DEFAULT '' COMMENT '头像URL',
    `gender` TINYINT DEFAULT 0 COMMENT '性别: 0未知 1男 2女',
    `birthday` VARCHAR(16) DEFAULT NULL COMMENT '生日',
    `signature` VARCHAR(256) DEFAULT '' COMMENT '个性签名',
    `salt` VARCHAR(64) DEFAULT NULL COMMENT '密码盐值',
    `status` TINYINT DEFAULT 0 COMMENT '账号状态: 0正常 1冻结 2注销',
    `vip_status` TINYINT DEFAULT 0 COMMENT 'VIP状态: 0非VIP 1VIP',
    `vip_expire_time` DATETIME DEFAULT NULL COMMENT 'VIP到期时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

DROP TABLE IF EXISTS `t_follow`;
CREATE TABLE `t_follow` (
    `id` BIGINT NOT NULL PRIMARY KEY COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `follow_user_id` BIGINT NOT NULL COMMENT '被关注用户ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_user_follow` (`user_id`, `follow_user_id`),
    KEY `idx_follow_user` (`follow_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='关注关系表';

-- --------------- 短剧服务 ---------------
DROP TABLE IF EXISTS `t_drama`;
CREATE TABLE `t_drama` (
    `id` BIGINT NOT NULL PRIMARY KEY,
    `title` VARCHAR(128) NOT NULL COMMENT '短剧标题',
    `cover` VARCHAR(512) DEFAULT '' COMMENT '封面图',
    `description` TEXT COMMENT '简介',
    `status` TINYINT DEFAULT 1 COMMENT '0下架 1上架',
    `completed` TINYINT DEFAULT 0 COMMENT '0连载中 1已完结',
    `pay_type` TINYINT DEFAULT 0 COMMENT '0免费 1付费 2部分付费',
    `price` INT DEFAULT 0 COMMENT '价格（金币）',
    `total_episodes` INT DEFAULT 0 COMMENT '总集数',
    `publisher_id` BIGINT DEFAULT NULL COMMENT '发行方ID',
    `views` BIGINT DEFAULT 0 COMMENT '播放量',
    `likes` BIGINT DEFAULT 0 COMMENT '点赞数',
    `collects` BIGINT DEFAULT 0 COMMENT '收藏数',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0 COMMENT '0未删除 1已删除',
    KEY `idx_status` (`status`, `deleted`),
    KEY `idx_title` (`title`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短剧表';

DROP TABLE IF EXISTS `t_episode`;
CREATE TABLE `t_episode` (
    `id` BIGINT NOT NULL PRIMARY KEY,
    `drama_id` BIGINT NOT NULL COMMENT '短剧ID',
    `title` VARCHAR(128) DEFAULT '' COMMENT '剧集标题',
    `episode_num` INT NOT NULL COMMENT '集号',
    `cover` VARCHAR(512) DEFAULT '' COMMENT '封面',
    `video_url` VARCHAR(512) DEFAULT '' COMMENT '视频地址',
    `duration` INT DEFAULT 0 COMMENT '时长（秒）',
    `free` TINYINT DEFAULT 0 COMMENT '0付费 1免费',
    `enabled` TINYINT DEFAULT 1 COMMENT '0禁用 1启用',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY `idx_drama_id` (`drama_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='剧集表';

DROP TABLE IF EXISTS `t_actor`;
CREATE TABLE `t_actor` (
    `id` BIGINT NOT NULL PRIMARY KEY,
    `name` VARCHAR(64) NOT NULL COMMENT '演员名称',
    `avatar` VARCHAR(512) DEFAULT '' COMMENT '头像',
    `description` TEXT COMMENT '简介',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='演员表';

DROP TABLE IF EXISTS `t_drama_actor`;
CREATE TABLE `t_drama_actor` (
    `id` BIGINT NOT NULL PRIMARY KEY,
    `drama_id` BIGINT NOT NULL,
    `actor_id` BIGINT NOT NULL,
    `role_name` VARCHAR(64) DEFAULT '' COMMENT '角色名',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_drama_actor` (`drama_id`, `actor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短剧演员关联表';

DROP TABLE IF EXISTS `t_tag`;
CREATE TABLE `t_tag` (
    `id` BIGINT NOT NULL PRIMARY KEY,
    `name` VARCHAR(32) NOT NULL COMMENT '标签名称',
    `parent_id` BIGINT DEFAULT 0 COMMENT '父标签ID 0=一级',
    `level` TINYINT DEFAULT 1 COMMENT '层级',
    `type` VARCHAR(16) DEFAULT 'drama' COMMENT '类型: drama/actor/user',
    `sort_order` INT DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签表';

DROP TABLE IF EXISTS `t_drama_tag`;
CREATE TABLE `t_drama_tag` (
    `id` BIGINT NOT NULL PRIMARY KEY,
    `drama_id` BIGINT NOT NULL,
    `tag_id` BIGINT NOT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_drama_tag` (`drama_id`, `tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短剧标签关联表';

DROP TABLE IF EXISTS `t_publisher`;
CREATE TABLE `t_publisher` (
    `id` BIGINT NOT NULL PRIMARY KEY,
    `name` VARCHAR(128) NOT NULL COMMENT '发行方名称',
    `logo` VARCHAR(512) DEFAULT '' COMMENT 'Logo',
    `description` TEXT COMMENT '简介',
    `contact` VARCHAR(128) DEFAULT '' COMMENT '联系方式',
    `qualification` VARCHAR(512) DEFAULT '' COMMENT '资质',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='发行方表';

-- --------------- 视频服务 ---------------
DROP TABLE IF EXISTS `t_video`;
CREATE TABLE `t_video` (
    `id` BIGINT NOT NULL PRIMARY KEY,
    `episode_id` BIGINT NOT NULL,
    `original_url` VARCHAR(512) DEFAULT '' COMMENT '原始视频地址',
    `status` TINYINT DEFAULT 0 COMMENT '0待转码 1转码中 2已完成 3失败',
    `resolutions` TEXT COMMENT 'JSON: 多清晰度信息',
    `hls_url` VARCHAR(512) DEFAULT '' COMMENT 'HLS播放地址',
    `cover_url` VARCHAR(512) DEFAULT '' COMMENT '视频封面',
    `duration` BIGINT DEFAULT 0 COMMENT '时长（秒）',
    `size` BIGINT DEFAULT 0 COMMENT '文件大小（字节）',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY `idx_episode` (`episode_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频表';

DROP TABLE IF EXISTS `t_play_progress`;
CREATE TABLE `t_play_progress` (
    `id` BIGINT NOT NULL PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `episode_id` BIGINT NOT NULL,
    `drama_id` BIGINT NOT NULL,
    `position` DOUBLE DEFAULT 0 COMMENT '播放位置（秒）',
    `duration` DOUBLE DEFAULT 0 COMMENT '总时长',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_user_episode` (`user_id`, `episode_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='播放进度表';

DROP TABLE IF EXISTS `t_watch_history`;
CREATE TABLE `t_watch_history` (
    `id` BIGINT NOT NULL PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `episode_id` BIGINT NOT NULL,
    `drama_id` BIGINT NOT NULL,
    `watch_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_user_time` (`user_id`, `watch_time` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='观看历史表';

-- --------------- 搜索服务 ---------------
DROP TABLE IF EXISTS `t_search_history`;
CREATE TABLE `t_search_history` (
    `id` BIGINT NOT NULL PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `keyword` VARCHAR(128) NOT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='搜索历史表';

DROP TABLE IF EXISTS `t_hot_word`;
CREATE TABLE `t_hot_word` (
    `id` BIGINT NOT NULL PRIMARY KEY,
    `word` VARCHAR(64) NOT NULL,
    `sort_order` INT DEFAULT 0,
    `enabled` TINYINT DEFAULT 1,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='热词表';

-- --------------- 互动服务 ---------------
DROP TABLE IF EXISTS `t_like`;
CREATE TABLE `t_like` (
    `id` BIGINT NOT NULL PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `drama_id` BIGINT NOT NULL,
    `status` TINYINT DEFAULT 1 COMMENT '0取消 1点赞',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_user_drama` (`user_id`, `drama_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='点赞表';

DROP TABLE IF EXISTS `t_collect`;
CREATE TABLE `t_collect` (
    `id` BIGINT NOT NULL PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `drama_id` BIGINT NOT NULL,
    `status` TINYINT DEFAULT 1 COMMENT '0取消 1收藏',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_user_drama` (`user_id`, `drama_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏表';

DROP TABLE IF EXISTS `t_comment`;
CREATE TABLE `t_comment` (
    `id` BIGINT NOT NULL PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `drama_id` BIGINT NOT NULL,
    `episode_id` BIGINT DEFAULT NULL,
    `parent_id` BIGINT DEFAULT 0 COMMENT '0=一级评论',
    `reply_to_user_id` BIGINT DEFAULT NULL,
    `content` TEXT NOT NULL,
    `like_count` INT DEFAULT 0,
    `reply_count` INT DEFAULT 0,
    `is_top` TINYINT DEFAULT 0 COMMENT '0否 1是',
    `is_deleted` TINYINT DEFAULT 0 COMMENT '0否 1是',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY `idx_drama` (`drama_id`, `parent_id`),
    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';

DROP TABLE IF EXISTS `t_comment_like`;
CREATE TABLE `t_comment_like` (
    `id` BIGINT NOT NULL PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `comment_id` BIGINT NOT NULL,
    `status` TINYINT DEFAULT 1 COMMENT '0取消 1点赞',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_user_comment` (`user_id`, `comment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论点赞表';

DROP TABLE IF EXISTS `t_danmaku`;
CREATE TABLE `t_danmaku` (
    `id` BIGINT NOT NULL PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `drama_id` BIGINT NOT NULL,
    `episode_id` BIGINT NOT NULL,
    `content` VARCHAR(256) NOT NULL,
    `play_time` DOUBLE NOT NULL COMMENT '弹幕时间点（秒）',
    `color` VARCHAR(16) DEFAULT '#FFFFFF',
    `font` INT DEFAULT 1,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_episode_time` (`episode_id`, `play_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='弹幕表';

DROP TABLE IF EXISTS `t_share`;
CREATE TABLE `t_share` (
    `id` BIGINT NOT NULL PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `drama_id` BIGINT NOT NULL,
    `platform` VARCHAR(32) DEFAULT '' COMMENT 'wechat/weibo/qq',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_drama` (`drama_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分享记录表';

-- --------------- 会员服务 ---------------
DROP TABLE IF EXISTS `t_vip_plan`;
CREATE TABLE `t_vip_plan` (
    `id` BIGINT NOT NULL PRIMARY KEY,
    `name` VARCHAR(64) NOT NULL,
    `price` INT NOT NULL COMMENT '价格（分）',
    `duration_days` INT NOT NULL COMMENT '有效天数',
    `description` VARCHAR(256) DEFAULT '',
    `enabled` TINYINT DEFAULT 1,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='VIP套餐表';

DROP TABLE IF EXISTS `t_vip_order`;
CREATE TABLE `t_vip_order` (
    `id` BIGINT NOT NULL PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `plan_id` BIGINT NOT NULL,
    `order_no` VARCHAR(64) NOT NULL COMMENT '订单号',
    `amount` INT NOT NULL COMMENT '金额（分）',
    `status` TINYINT DEFAULT 0 COMMENT '0待支付 1已支付 2已取消 3已退款',
    `pay_time` DATETIME DEFAULT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='VIP订单表';

DROP TABLE IF EXISTS `t_vip_record`;
CREATE TABLE `t_vip_record` (
    `id` BIGINT NOT NULL PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `order_id` BIGINT DEFAULT NULL,
    `start_time` DATETIME NOT NULL,
    `end_time` DATETIME NOT NULL,
    `source` TINYINT DEFAULT 0 COMMENT '0购买 1广告 2赠送',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='VIP开通记录表';

DROP TABLE IF EXISTS `t_user_wallet`;
CREATE TABLE `t_user_wallet` (
    `id` BIGINT NOT NULL PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `balance` BIGINT DEFAULT 0 COMMENT '金币余额',
    `frozen_balance` BIGINT DEFAULT 0 COMMENT '冻结金币',
    `version` INT DEFAULT 0 COMMENT '乐观锁版本号',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户钱包表';

DROP TABLE IF EXISTS `t_recharge_order`;
CREATE TABLE `t_recharge_order` (
    `id` BIGINT NOT NULL PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `order_no` VARCHAR(64) NOT NULL COMMENT '订单号',
    `amount` BIGINT NOT NULL COMMENT '金币数量',
    `money` INT NOT NULL COMMENT '金额（分）',
    `status` TINYINT DEFAULT 0 COMMENT '0待支付 1已支付 2已取消 3已退款',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='充值订单表';

DROP TABLE IF EXISTS `t_consume_record`;
CREATE TABLE `t_consume_record` (
    `id` BIGINT NOT NULL PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `order_no` VARCHAR(64) NOT NULL,
    `amount` BIGINT NOT NULL COMMENT '金币',
    `type` TINYINT DEFAULT 0 COMMENT '0消费 1退款',
    `drama_id` BIGINT DEFAULT NULL,
    `description` VARCHAR(256) DEFAULT '',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消费记录表';

DROP TABLE IF EXISTS `t_wallet_transaction`;
CREATE TABLE `t_wallet_transaction` (
    `id` BIGINT NOT NULL PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `amount` BIGINT NOT NULL,
    `before_balance` BIGINT NOT NULL,
    `after_balance` BIGINT NOT NULL,
    `type` VARCHAR(16) NOT NULL COMMENT 'recharge/consume/refund',
    `order_no` VARCHAR(64) NOT NULL,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_user_time` (`user_id`, `create_time` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='钱包流水表';

-- --------------- 数据服务 ---------------
DROP TABLE IF EXISTS `t_play_stat`;
CREATE TABLE `t_play_stat` (
    `id` BIGINT NOT NULL PRIMARY KEY,
    `drama_id` BIGINT NOT NULL,
    `type` VARCHAR(16) NOT NULL COMMENT 'day/week/month/total',
    `stat_date` VARCHAR(16) NOT NULL,
    `count` BIGINT DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_drama_type_date` (`drama_id`, `type`, `stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='播放统计表';

-- --------------- 营销服务 ---------------
DROP TABLE IF EXISTS `t_user_checkin`;
CREATE TABLE `t_user_checkin` (
    `id` BIGINT NOT NULL PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `checkin_date` DATE NOT NULL COMMENT '签到日期',
    `consecutive_days` INT DEFAULT 1 COMMENT '连续签到天数',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_user_date` (`user_id`, `checkin_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='签到记录表';

DROP TABLE IF EXISTS `t_task`;
CREATE TABLE `t_task` (
    `id` BIGINT NOT NULL PRIMARY KEY,
    `name` VARCHAR(64) NOT NULL,
    `type` VARCHAR(32) NOT NULL COMMENT 'watch/signup/share',
    `description` VARCHAR(256) DEFAULT '',
    `duration_required` INT DEFAULT 0 COMMENT '观看时长要求（秒）',
    `reward_coin` INT DEFAULT 0 COMMENT '奖励金币',
    `enabled` TINYINT DEFAULT 1,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务表';

DROP TABLE IF EXISTS `t_user_task`;
CREATE TABLE `t_user_task` (
    `id` BIGINT NOT NULL PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `task_id` BIGINT NOT NULL,
    `status` TINYINT DEFAULT 0 COMMENT '0进行中 1已完成 2已领取',
    `progress` INT DEFAULT 0 COMMENT '进度',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_user_task` (`user_id`, `task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户任务表';

DROP TABLE IF EXISTS `t_coupon_template`;
CREATE TABLE `t_coupon_template` (
    `id` BIGINT NOT NULL PRIMARY KEY,
    `name` VARCHAR(64) NOT NULL,
    `type` VARCHAR(16) NOT NULL COMMENT 'full_subtract/discount/cash',
    `face_value` INT NOT NULL COMMENT '面值（分/百分比）',
    `threshold` INT DEFAULT 0 COMMENT '使用门槛（分）',
    `total_stock` INT NOT NULL COMMENT '发行总量',
    `remaining_stock` INT NOT NULL COMMENT '剩余库存',
    `valid_days` INT DEFAULT 7 COMMENT '领取后N天有效',
    `valid_start` DATE DEFAULT NULL,
    `valid_end` DATE DEFAULT NULL,
    `scope` VARCHAR(128) DEFAULT '' COMMENT '适用范围',
    `enabled` TINYINT DEFAULT 1,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券模板表';

DROP TABLE IF EXISTS `t_coupon_instance`;
CREATE TABLE `t_coupon_instance` (
    `id` BIGINT NOT NULL PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `template_id` BIGINT NOT NULL,
    `status` TINYINT DEFAULT 0 COMMENT '0未使用 1已使用 2已过期 3已锁定',
    `order_no` VARCHAR(64) DEFAULT NULL,
    `receive_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `use_time` DATETIME DEFAULT NULL,
    `expire_time` DATETIME NOT NULL,
    KEY `idx_user_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券实例表';

-- --------------- 推送服务 ---------------
DROP TABLE IF EXISTS `t_notification`;
CREATE TABLE `t_notification` (
    `id` BIGINT NOT NULL PRIMARY KEY,
    `user_id` BIGINT NOT NULL COMMENT '0=全量用户',
    `title` VARCHAR(128) NOT NULL,
    `content` TEXT,
    `type` VARCHAR(16) DEFAULT 'system' COMMENT 'system/interaction/update',
    `is_read` TINYINT DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_user_read` (`user_id`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知表';

DROP TABLE IF EXISTS `t_push_record`;
CREATE TABLE `t_push_record` (
    `id` BIGINT NOT NULL PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `title` VARCHAR(128) NOT NULL,
    `content` TEXT,
    `push_type` VARCHAR(16) DEFAULT 'app' COMMENT 'app/sms/inapp',
    `push_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `result` VARCHAR(64) DEFAULT '' COMMENT 'success/failed',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推送记录表';

-- --------------- 主键生成服务 ---------------
DROP TABLE IF EXISTS `t_id_segment`;
CREATE TABLE `t_id_segment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `biz_type` VARCHAR(32) NOT NULL COMMENT '业务类型: user/drama/order/comment',
    `max_id` BIGINT NOT NULL DEFAULT 0 COMMENT '当前最大ID',
    `step` INT NOT NULL DEFAULT 1000 COMMENT '号段步长',
    `version` INT DEFAULT 0 COMMENT '乐观锁',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_biz_type` (`biz_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ID号段表';

-- 初始化号段数据
INSERT INTO `t_id_segment` (`biz_type`, `max_id`, `step`) VALUES
('user', 0, 1000),
('drama', 0, 1000),
('episode', 0, 1000),
('actor', 0, 500),
('tag', 0, 500),
('drama_actor', 0, 500),
('drama_tag', 0, 500),
('publisher', 0, 200),
('comment', 0, 1000),
('order', 0, 1000),
('video', 0, 500),
('admin', 0, 200);
