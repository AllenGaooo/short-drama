-- =====================================================
-- 兔兔短剧 (bunny-flix) 短剧域 SQL：建表 + 20 条演示数据
-- 说明：本文件聚焦「短剧服务」相关表，可直接在已创建
--       bunny_flix 库后执行。完整全量表结构见 init.sql。
-- =====================================================

USE `bunny_flix`;

-- --------------- 短剧域表结构 ---------------
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

-- =====================================================
-- 演示数据
-- =====================================================

-- --------------- 发行方（5 条） ---------------
INSERT INTO `t_publisher` (`id`, `name`, `logo`, `description`, `contact`, `qualification`) VALUES
(1, '甜宠文化传媒', 'https://cdn.bunny-flix.com/pub/1.png', '专注都市甜宠短剧出品', 'contact@tianchong.com', '影视制作许可证 A-001'),
(2, '星河影视', 'https://cdn.bunny-flix.com/pub/2.png', '古装与穿越题材头部厂牌', 'contact@xinghe.com', '影视制作许可证 A-002'),
(3, '逆袭工作室', 'https://cdn.bunny-flix.com/pub/3.png', '重生逆袭类精品短剧', 'contact@nixi.com', '影视制作许可证 A-003'),
(4, '豪门娱乐', 'https://cdn.bunny-flix.com/pub/4.png', '霸总豪门类高甜剧集', 'contact@haomen.com', '影视制作许可证 A-004'),
(5, '古风映画', 'https://cdn.bunny-flix.com/pub/5.png', '古装权谋与医妃系列', 'contact@gufeng.com', '影视制作许可证 A-005');

-- --------------- 演员（15 条） ---------------
INSERT INTO `t_actor` (`id`, `name`, `avatar`, `description`) VALUES
(1, '林婉儿', 'https://cdn.bunny-flix.com/actor/1.png', '新生代甜宠剧女主'),
(2, '顾廷烨', 'https://cdn.bunny-flix.com/actor/2.png', '霸总专业户男主'),
(3, '苏沐', 'https://cdn.bunny-flix.com/actor/3.png', '古装小生'),
(4, '陆景行', 'https://cdn.bunny-flix.com/actor/4.png', '都市剧人气男主'),
(5, '沈清欢', 'https://cdn.bunny-flix.com/actor/5.png', '重生题材实力派女主'),
(6, '霍言', 'https://cdn.bunny-flix.com/actor/6.png', '悬疑剧型男'),
(7, '白若雪', 'https://cdn.bunny-flix.com/actor/7.png', '古装花旦'),
(8, '江辰', 'https://cdn.bunny-flix.com/actor/8.png', '喜剧担当男主'),
(9, '顾倾城', 'https://cdn.bunny-flix.com/actor/9.png', '豪门剧御姐'),
(10, '萧煜', 'https://cdn.bunny-flix.com/actor/10.png', '穿越剧男主'),
(11, '沐晴天', 'https://cdn.bunny-flix.com/actor/11.png', '甜妹代表'),
(12, '厉司宴', 'https://cdn.bunny-flix.com/actor/12.png', '病娇角色专业户'),
(13, '苏晚晚', 'https://cdn.bunny-flix.com/actor/13.png', '逆袭剧女主'),
(14, '夜墨', 'https://cdn.bunny-flix.com/actor/14.png', '暗黑系男主'),
(15, '楚云汐', 'https://cdn.bunny-flix.com/actor/15.png', '医妃系列女主');

-- --------------- 标签（10 条） ---------------
INSERT INTO `t_tag` (`id`, `name`, `parent_id`, `level`, `type`, `sort_order`) VALUES
(1, '都市', 0, 1, 'drama', 1),
(2, '甜宠', 0, 1, 'drama', 2),
(3, '重生', 0, 1, 'drama', 3),
(4, '穿越', 0, 1, 'drama', 4),
(5, '霸总', 0, 1, 'drama', 5),
(6, '古装', 0, 1, 'drama', 6),
(7, '逆袭', 0, 1, 'drama', 7),
(8, '虐恋', 0, 1, 'drama', 8),
(9, '悬疑', 0, 1, 'drama', 9),
(10, '喜剧', 0, 1, 'drama', 10);

-- --------------- 短剧（20 条核心数据） ---------------
INSERT INTO `t_drama` (`id`, `title`, `cover`, `description`, `status`, `completed`, `pay_type`, `price`, `total_episodes`, `publisher_id`, `views`, `likes`, `collects`) VALUES
(1,  '总裁的隐婚甜妻', 'https://cdn.bunny-flix.com/cover/1.jpg', '落难千金隐婚霸总，先婚后爱甜到齁。', 1, 1, 2, 99, 80, 4, 1280000, 86000, 42000),
(2,  '重生之逆袭人生', 'https://cdn.bunny-flix.com/cover/2.jpg', '重生归来，弥补遗憾走上人生巅峰。', 1, 1, 1, 199, 100, 3, 2150000, 153000, 88000),
(3,  '千金归来之霸道CEO', 'https://cdn.bunny-flix.com/cover/3.jpg', '豪门千金归来的复仇与真爱。', 1, 0, 2, 99, 60, 4, 980000, 67000, 31000),
(4,  '闪婚娇妻太撩人', 'https://cdn.bunny-flix.com/cover/4.jpg', '一场闪婚，撩动冰山总裁的心。', 1, 1, 0, 0, 70, 1, 1760000, 120000, 65000),
(5,  '我在八零年代当首富', 'https://cdn.bunny-flix.com/cover/5.jpg', '穿越回八零，从零开始缔造商业帝国。', 1, 1, 1, 159, 90, 2, 1890000, 132000, 72000),
(6,  '病娇王爷的替代品', 'https://cdn.bunny-flix.com/cover/6.jpg', '替身入府，与病娇王爷的虐恋纠缠。', 1, 0, 2, 99, 55, 5, 760000, 51000, 28000),
(7,  '离婚后我成了大佬的白月光', 'https://cdn.bunny-flix.com/cover/7.jpg', '离婚后的她，被神秘大佬捧在手心。', 1, 1, 2, 129, 85, 1, 2030000, 145000, 81000),
(8,  '豪门少夫人别跑', 'https://cdn.bunny-flix.com/cover/8.jpg', '逃婚少夫人与腹黑少爷的追逐战。', 1, 0, 0, 0, 65, 4, 1120000, 78000, 39000),
(9,  '穿越之农门医女', 'https://cdn.bunny-flix.com/cover/9.jpg', '现代医生穿越农门，悬壶济世闯出一片天。', 1, 1, 1, 169, 95, 2, 1670000, 118000, 64000),
(10, '暗夜里的守护者', 'https://cdn.bunny-flix.com/cover/10.jpg', '悬疑推理，揭开城市背后的守护秘密。', 1, 0, 1, 199, 48, 1, 540000, 38000, 21000),
(11, '逆袭吧！废柴大小姐', 'https://cdn.bunny-flix.com/cover/11.jpg', '废柴逆袭，打脸渣男赢回尊严。', 1, 1, 2, 99, 78, 3, 1450000, 99000, 53000),
(12, '总裁的神秘新娘', 'https://cdn.bunny-flix.com/cover/12.jpg', '神秘新娘身份成谜，霸总步步深陷。', 1, 1, 0, 0, 72, 4, 1980000, 139000, 76000),
(13, '末世重生之女王归来', 'https://cdn.bunny-flix.com/cover/13.jpg', '末世重生，冷酷女王带领人类绝地反击。', 1, 0, 1, 219, 88, 3, 1320000, 91000, 47000),
(14, '高糖闪婚契约', 'https://cdn.bunny-flix.com/cover/14.jpg', '契约婚姻里的高糖日常。', 1, 1, 2, 89, 60, 1, 1690000, 116000, 60000),
(15, '我的霸总男友', 'https://cdn.bunny-flix.com/cover/15.jpg', '普通女孩与霸总的甜宠恋爱日常。', 1, 1, 0, 0, 66, 4, 2210000, 158000, 84000),
(16, '医妃惊华', 'https://cdn.bunny-flix.com/cover/16.jpg', '医妃出诊，惊艳朝堂与后宫。', 1, 1, 1, 179, 92, 5, 1540000, 107000, 58000),
(17, '重生后我养大了反派', 'https://cdn.bunny-flix.com/cover/17.jpg', '重生后她决定亲手养大那个反派。', 1, 0, 2, 99, 70, 3, 1010000, 70000, 36000),
(18, '首席的落跑甜心', 'https://cdn.bunny-flix.com/cover/18.jpg', '落跑甜心被首席强势宠回。', 1, 1, 0, 0, 58, 1, 1870000, 130000, 69000),
(19, '都市最强神医', 'https://cdn.bunny-flix.com/cover/19.jpg', '隐世神医入都市，妙手回春震八方。', 1, 0, 1, 159, 76, 2, 1190000, 83000, 44000),
(20, '摄政王的掌中娇', 'https://cdn.bunny-flix.com/cover/20.jpg', '摄政王独宠，掌中娇妻惹人羡。', 1, 1, 2, 109, 82, 5, 1360000, 95000, 50000);

-- --------------- 剧集（每剧 2 集，共 40 条） ---------------
INSERT INTO `t_episode` (`id`, `drama_id`, `title`, `episode_num`, `cover`, `video_url`, `duration`, `free`, `enabled`, `sort_order`) VALUES
(101, 1, '第1集 隐婚协议', 1, 'https://cdn.bunny-flix.com/ep/1_1.jpg', 'https://cdn.bunny-flix.com/v/1_1.m3u8', 95, 1, 1, 1),
(102, 1, '第2集 总裁的试探', 2, 'https://cdn.bunny-flix.com/ep/1_2.jpg', 'https://cdn.bunny-flix.com/v/1_2.m3u8', 98, 0, 1, 2),
(103, 2, '第1集 重生归来', 1, 'https://cdn.bunny-flix.com/ep/2_1.jpg', 'https://cdn.bunny-flix.com/v/2_1.m3u8', 102, 1, 1, 1),
(104, 2, '第2集 打脸前夫', 2, 'https://cdn.bunny-flix.com/ep/2_2.jpg', 'https://cdn.bunny-flix.com/v/2_2.m3u8', 105, 0, 1, 2),
(105, 3, '第1集 千金归来', 1, 'https://cdn.bunny-flix.com/ep/3_1.jpg', 'https://cdn.bunny-flix.com/v/3_1.m3u8', 92, 1, 1, 1),
(106, 3, '第2集 初遇CEO', 2, 'https://cdn.bunny-flix.com/ep/3_2.jpg', 'https://cdn.bunny-flix.com/v/3_2.m3u8', 96, 0, 1, 2),
(107, 4, '第1集 闪婚现场', 1, 'https://cdn.bunny-flix.com/ep/4_1.jpg', 'https://cdn.bunny-flix.com/v/4_1.m3u8', 88, 1, 1, 1),
(108, 4, '第2集 撩动冰山', 2, 'https://cdn.bunny-flix.com/ep/4_2.jpg', 'https://cdn.bunny-flix.com/v/4_2.m3u8', 90, 1, 1, 2),
(109, 5, '第1集 穿越八零', 1, 'https://cdn.bunny-flix.com/ep/5_1.jpg', 'https://cdn.bunny-flix.com/v/5_1.m3u8', 110, 1, 1, 1),
(110, 5, '第2集 第一桶金', 2, 'https://cdn.bunny-flix.com/ep/5_2.jpg', 'https://cdn.bunny-flix.com/v/5_2.m3u8', 112, 0, 1, 2),
(111, 6, '第1集 替身入府', 1, 'https://cdn.bunny-flix.com/ep/6_1.jpg', 'https://cdn.bunny-flix.com/v/6_1.m3u8', 99, 1, 1, 1),
(112, 6, '第2集 王爷的执念', 2, 'https://cdn.bunny-flix.com/ep/6_2.jpg', 'https://cdn.bunny-flix.com/v/6_2.m3u8', 101, 0, 1, 2),
(113, 7, '第1集 离婚之后', 1, 'https://cdn.bunny-flix.com/ep/7_1.jpg', 'https://cdn.bunny-flix.com/v/7_1.m3u8', 94, 1, 1, 1),
(114, 7, '第2集 大佬现身', 2, 'https://cdn.bunny-flix.com/ep/7_2.jpg', 'https://cdn.bunny-flix.com/v/7_2.m3u8', 97, 0, 1, 2),
(115, 8, '第1集 逃婚少夫人', 1, 'https://cdn.bunny-flix.com/ep/8_1.jpg', 'https://cdn.bunny-flix.com/v/8_1.m3u8', 91, 1, 1, 1),
(116, 8, '第2集 少爷追来', 2, 'https://cdn.bunny-flix.com/ep/8_2.jpg', 'https://cdn.bunny-flix.com/v/8_2.m3u8', 93, 1, 1, 2),
(117, 9, '第1集 医女穿越', 1, 'https://cdn.bunny-flix.com/ep/9_1.jpg', 'https://cdn.bunny-flix.com/v/9_1.m3u8', 108, 1, 1, 1),
(118, 9, '第2集 妙手回春', 2, 'https://cdn.bunny-flix.com/ep/9_2.jpg', 'https://cdn.bunny-flix.com/v/9_2.m3u8', 109, 0, 1, 2),
(119, 10, '第1集 暗夜疑云', 1, 'https://cdn.bunny-flix.com/ep/10_1.jpg', 'https://cdn.bunny-flix.com/v/10_1.m3u8', 120, 1, 1, 1),
(120, 10, '第2集 守护者现身', 2, 'https://cdn.bunny-flix.com/ep/10_2.jpg', 'https://cdn.bunny-flix.com/v/10_2.m3u8', 122, 0, 1, 2),
(121, 11, '第1集 废柴觉醒', 1, 'https://cdn.bunny-flix.com/ep/11_1.jpg', 'https://cdn.bunny-flix.com/v/11_1.m3u8', 96, 1, 1, 1),
(122, 11, '第2集 当众打脸', 2, 'https://cdn.bunny-flix.com/ep/11_2.jpg', 'https://cdn.bunny-flix.com/v/11_2.m3u8', 98, 0, 1, 2),
(123, 12, '第1集 神秘新娘', 1, 'https://cdn.bunny-flix.com/ep/12_1.jpg', 'https://cdn.bunny-flix.com/v/12_1.m3u8', 93, 1, 1, 1),
(124, 12, '第2集 身份成谜', 2, 'https://cdn.bunny-flix.com/ep/12_2.jpg', 'https://cdn.bunny-flix.com/v/12_2.m3u8', 95, 1, 1, 2),
(125, 13, '第1集 末世重生', 1, 'https://cdn.bunny-flix.com/ep/13_1.jpg', 'https://cdn.bunny-flix.com/v/13_1.m3u8', 115, 1, 1, 1),
(126, 13, '第2集 女王集结', 2, 'https://cdn.bunny-flix.com/ep/13_2.jpg', 'https://cdn.bunny-flix.com/v/13_2.m3u8', 117, 0, 1, 2),
(127, 14, '第1集 契约婚姻', 1, 'https://cdn.bunny-flix.com/ep/14_1.jpg', 'https://cdn.bunny-flix.com/v/14_1.m3u8', 89, 1, 1, 1),
(128, 14, '第2集 高糖日常', 2, 'https://cdn.bunny-flix.com/ep/14_2.jpg', 'https://cdn.bunny-flix.com/v/14_2.m3u8', 91, 0, 1, 2),
(129, 15, '第1集 普通女孩', 1, 'https://cdn.bunny-flix.com/ep/15_1.jpg', 'https://cdn.bunny-flix.com/v/15_1.m3u8', 87, 1, 1, 1),
(130, 15, '第2集 霸总心动', 2, 'https://cdn.bunny-flix.com/ep/15_2.jpg', 'https://cdn.bunny-flix.com/v/15_2.m3u8', 90, 1, 1, 2),
(131, 16, '第1集 医妃入宫', 1, 'https://cdn.bunny-flix.com/ep/16_1.jpg', 'https://cdn.bunny-flix.com/v/16_1.m3u8', 105, 1, 1, 1),
(132, 16, '第2集 惊艳朝堂', 2, 'https://cdn.bunny-flix.com/ep/16_2.jpg', 'https://cdn.bunny-flix.com/v/16_2.m3u8', 107, 0, 1, 2),
(133, 17, '第1集 再遇反派', 1, 'https://cdn.bunny-flix.com/ep/17_1.jpg', 'https://cdn.bunny-flix.com/v/17_1.m3u8', 97, 1, 1, 1),
(134, 17, '第2集 开始养成', 2, 'https://cdn.bunny-flix.com/ep/17_2.jpg', 'https://cdn.bunny-flix.com/v/17_2.m3u8', 99, 0, 1, 2),
(135, 18, '第1集 落跑甜心', 1, 'https://cdn.bunny-flix.com/ep/18_1.jpg', 'https://cdn.bunny-flix.com/v/18_1.m3u8', 92, 1, 1, 1),
(136, 18, '第2集 首席宠回', 2, 'https://cdn.bunny-flix.com/ep/18_2.jpg', 'https://cdn.bunny-flix.com/v/18_2.m3u8', 94, 1, 1, 2),
(137, 19, '第1集 神医入都市', 1, 'https://cdn.bunny-flix.com/ep/19_1.jpg', 'https://cdn.bunny-flix.com/v/19_1.m3u8', 101, 1, 1, 1),
(138, 19, '第2集 妙手震八方', 2, 'https://cdn.bunny-flix.com/ep/19_2.jpg', 'https://cdn.bunny-flix.com/v/19_2.m3u8', 103, 0, 1, 2),
(139, 20, '第1集 掌中娇妻', 1, 'https://cdn.bunny-flix.com/ep/20_1.jpg', 'https://cdn.bunny-flix.com/v/20_1.m3u8', 98, 1, 1, 1),
(140, 20, '第2集 摄政王独宠', 2, 'https://cdn.bunny-flix.com/ep/20_2.jpg', 'https://cdn.bunny-flix.com/v/20_2.m3u8', 100, 0, 1, 2);

-- --------------- 短剧-演员关联（每剧 2 位，共 40 条） ---------------
INSERT INTO `t_drama_actor` (`id`, `drama_id`, `actor_id`, `role_name`) VALUES
(1001, 1, 1, '苏晚（女主）'), (1002, 1, 2, '顾总（男主）'),
(1003, 2, 5, '重生女主'),     (1004, 2, 4, '陆衍（男主）'),
(1005, 3, 9, '千金（女主）'), (1006, 3, 2, 'CEO（男主）'),
(1007, 4, 11, '娇妻（女主）'),(1008, 4, 4, '冰山总裁'),
(1009, 5, 13, '首富（女主）'),(1010, 5, 10, '穿越男主'),
(1011, 6, 7, '替身（女主）'), (1012, 6, 12, '病娇王爷'),
(1013, 7, 5, '白月光（女主）'),(1014, 7, 14, '神秘大佬'),
(1015, 8, 11, '少夫人（女主）'),(1016, 8, 2, '腹黑少爷'),
(1017, 9, 15, '医女（女主）'), (1018, 9, 3, '农门男主'),
(1019, 10, 6, '守护者（男主）'),(1020, 10, 1, '女侦探'),
(1021, 11, 13, '废柴（女主）'),(1022, 11, 8, '逆袭助力'),
(1023, 12, 9, '神秘新娘'),     (1024, 12, 2, '霸总'),
(1025, 13, 5, '末世女王'),     (1026, 13, 14, '反派领袖'),
(1027, 14, 11, '甜心（女主）'),(1028, 14, 4, '首席（男主）'),
(1029, 15, 1, '普通女孩'),     (1030, 15, 2, '霸总男友'),
(1031, 16, 15, '医妃（女主）'),(1032, 16, 3, '摄政王'),
(1033, 17, 13, '重生女主'),     (1034, 17, 12, '反派（养成中）'),
(1035, 18, 11, '落跑甜心'),     (1036, 18, 4, '首席'),
(1037, 19, 7, '神医（女主）'), (1038, 19, 6, '都市男主'),
(1039, 20, 15, '掌中娇（女主）'),(1040, 20, 3, '摄政王');

-- --------------- 短剧-标签关联（每剧 2 个，共 40 条） ---------------
INSERT INTO `t_drama_tag` (`id`, `drama_id`, `tag_id`) VALUES
(2001, 1, 5), (2002, 1, 2),
(2003, 2, 3), (2004, 2, 7),
(2005, 3, 5), (2006, 3, 1),
(2007, 4, 2), (2008, 4, 1),
(2009, 5, 4), (2010, 5, 7),
(2011, 6, 6), (2012, 6, 8),
(2013, 7, 1), (2014, 7, 2),
(2015, 8, 5), (2016, 8, 2),
(2017, 9, 4), (2018, 9, 6),
(2019, 10, 9), (2020, 10, 1),
(2021, 11, 7), (2022, 11, 8),
(2023, 12, 5), (2024, 12, 2),
(2025, 13, 3), (2026, 13, 9),
(2027, 14, 2), (2028, 14, 5),
(2029, 15, 5), (2030, 15, 2),
(2031, 16, 6), (2032, 16, 4),
(2033, 17, 3), (2034, 17, 7),
(2035, 18, 2), (2036, 18, 1),
(2037, 19, 1), (2038, 19, 10),
(2039, 20, 6), (2040, 20, 8);
