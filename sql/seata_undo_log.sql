-- =====================================================
-- Seata undo_log 表（业务库）
--
-- 说明：
-- 1. 本项目 VIP 购买用 Seata TCC 模式，TCC 的回滚由业务代码（Cancel 方法）实现，
--    本身不需要 undo_log。
-- 2. 但 spring-cloud-starter-alibaba-seata 默认开启数据源代理
--    （seata.enable-auto-data-source-proxy 默认 true），
--    全局事务入口方法里的普通 DB 操作（如创建订单 vip_order.insert）
--    会作为 AT 分支注册，此时需要 undo_log 记录回滚日志。
-- 3. 因此只要用了 @GlobalTransactional，就应在业务库建这张表。
--
-- 执行：mysql -u root bunny_flix < sql/seata_undo_log.sql
-- =====================================================

USE `bunny_flix`;

DROP TABLE IF EXISTS `undo_log`;
CREATE TABLE `undo_log` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `branch_id`     BIGINT       NOT NULL COMMENT '分支事务ID',
    `xid`           VARCHAR(100) NOT NULL COMMENT '全局事务ID',
    `context`       VARCHAR(128) NOT NULL COMMENT '上下文',
    `rollback_info` LONGBLOB     NOT NULL COMMENT '回滚信息',
    `log_status`    INT          NOT NULL COMMENT '日志状态',
    `log_created`   DATETIME     NOT NULL COMMENT '创建时间',
    `log_modified`  DATETIME     NOT NULL COMMENT '修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `ux_undo_log` (`xid`, `branch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Seata 回滚日志表';
