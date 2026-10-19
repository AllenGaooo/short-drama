-- =====================================================
-- 兔兔短剧 (bunny-flix) 后台管理模块建表脚本
-- 包含：后台管理员表 t_admin_user + 后台菜单表 admin_menu
-- 与普通用户（t_user）完全隔离，专用于管理后台
-- 管理员ID 由 id-generator (ADMIN 号段) 分配
-- 执行：mysql -u root bunny_flix < sql/admin_user.sql
-- =====================================================

USE `bunny_flix`;

-- --------------- 后台管理员用户表 ---------------
DROP TABLE IF EXISTS `t_admin_user`;
CREATE TABLE `t_admin_user` (
    `id`          BIGINT       NOT NULL COMMENT '管理员ID（来自 id-generator ADMIN 号段）',
    `phone`       VARCHAR(20)  NOT NULL COMMENT '登录手机号',
    `password`    VARCHAR(64)  NOT NULL COMMENT '密码（MD5(密码+盐)）',
    `nickname`    VARCHAR(64)  DEFAULT '' COMMENT '昵称',
    `avatar`      VARCHAR(255) DEFAULT '' COMMENT '头像URL',
    `salt`        VARCHAR(32)  NOT NULL COMMENT '密码盐值',
    `status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0正常 1冻结 2注销',
    `role`        VARCHAR(32)  DEFAULT 'NORMAL' COMMENT '角色：SUPER / NORMAL',
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='后台管理员用户表';

-- 初始化一个超级管理员（手机号 13800000000 / 密码 admin1234）
-- 密码 = MD5('admin1234' + salt)，salt 见下方；如需改密码请重新计算
INSERT INTO `t_admin_user` (`id`, `phone`, `password`, `salt`, `nickname`, `status`, `role`)
VALUES (1, '13800000000', MD5(CONCAT('admin1234', 'initadminsalt000')), 'initadminsalt000', '超级管理员', 0, 'SUPER');

-- --------------- 后台菜单表 ---------------
DROP TABLE IF EXISTS `admin_menu`;
CREATE TABLE `admin_menu` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '菜单ID',
    `user_id`     BIGINT       NOT NULL COMMENT '关联管理员ID（t_admin_user.id），菜单归属/授权给哪个管理员',
    `parent_id`   BIGINT       NOT NULL DEFAULT 0 COMMENT '父菜单ID，0表示一级菜单（自关联）',
    `name`        VARCHAR(64)  NOT NULL COMMENT '菜单名称',
    `type`        TINYINT      NOT NULL DEFAULT 1 COMMENT '菜单类型: 1目录 2菜单(页面) 3按钮(权限)',
    `path`        VARCHAR(128) DEFAULT '' COMMENT '前端路由路径，如 /user/list',
    `component`   VARCHAR(128) DEFAULT '' COMMENT '前端组件路径，如 views/user/UserList.vue',
    `icon`        VARCHAR(64)  DEFAULT '' COMMENT '图标',
    `permission`  VARCHAR(64)  DEFAULT '' COMMENT '权限标识，如 user:list（按钮级控制）',
    `sort`        INT          NOT NULL DEFAULT 0 COMMENT '排序号，越小越靠前',
    `visible`     TINYINT      NOT NULL DEFAULT 1 COMMENT '是否显示: 1显示 0隐藏',
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY `idx_user_id` (`user_id`),
    KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理后台菜单表（关联管理员）';

-- 初始化示例：给管理员ID=1（上方超级管理员）分配一套基础菜单
INSERT INTO `admin_menu` (`user_id`, `parent_id`, `name`, `type`, `path`, `component`, `icon`, `permission`, `sort`, `visible`) VALUES
(1, 0, '用户管理',   1, '/user',              '',                           'User',    '',           1, 1),
(1, 1, '用户列表',   2, '/user/list',         'views/user/UserList.vue',    'List',    'user:list',   1, 1),
(1, 1, '新增用户',   3, '',                   '',                          '',        'user:add',    2, 1),
(1, 0, '短剧管理',   1, '/drama',             '',                           'Video',   '',           2, 1),
(1, 4, '短剧列表',   2, '/drama/list',        'views/drama/DramaList.vue',  'List',    'drama:list',  1, 1),
(1, 4, '新增短剧',   3, '',                   '',                          '',        'drama:add',   2, 1),
(1, 0, '营销管理',   1, '/marketing',         '',                           'Present', '',           3, 1),
(1, 7, '优惠券管理', 2, '/marketing/coupon',  'views/marketing/Coupon.vue', 'Ticket',  'coupon:list', 1, 1),
(1, 0, '智能助手',   1, '/agent',             '',                           'Agent',   '',           4, 1),
(1, 10, '对话Agent', 2, '/agent/chat',        'views/agent/AgentChat.vue',  'ChatDotRound', 'agent:chat', 1, 1),
(1, 10, 'AI网关对话', 2, '/agent/aigateway',   'views/agent/AiGatewayChat.vue', 'ChatDotRound', 'aigateway:chat', 2, 1);
