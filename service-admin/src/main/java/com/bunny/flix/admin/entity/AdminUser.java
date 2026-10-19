package com.bunny.flix.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 后台管理员用户实体类，对应数据库 t_admin_user 表
 * 与普通用户（t_user）完全隔离，专用于管理后台登录
 */
@Data
@TableName("t_admin_user")
public class AdminUser {

    /** 管理员ID，由 service-id-generator 号段模式分配（ADMIN 业务） */
    @TableId(type = IdType.INPUT)
    private Long id;

    /** 登录手机号 */
    private String phone;

    /** 密码（加密后的） */
    private String password;

    /** 昵称 */
    private String nickname;

    /** 头像URL */
    private String avatar;

    /** 密码盐值 */
    private String salt;

    /** 账号状态：0-正常，1-冻结，2-已注销 */
    private Integer status;

    /** 角色：如 SUPER、NORMAL */
    private String role;

    /** 逻辑删除：0未删除 1已删除（由 yml 全局 logic-delete-field: deleted 生效） */
    private Integer deleted;

    /** 注册时间 */
    private LocalDateTime createTime;

    /** 最后更新时间 */
    private LocalDateTime updateTime;
}
