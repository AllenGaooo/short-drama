package com.bunny.flix.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 后台菜单实体类，对应数据库 admin_menu 表
 * 关联管理员用户（t_admin_user.id），支持多级菜单（目录/页面/按钮）
 */
@Data
@TableName("admin_menu")
public class AdminMenu {

    /** 菜单ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联管理员用户ID（t_admin_user.id） */
    private Long userId;

    /** 父菜单ID，0表示一级菜单 */
    private Long parentId;

    /** 菜单名称 */
    private String name;

    /** 菜单类型：1目录 2菜单(页面) 3按钮(权限) */
    private Integer type;

    /** 前端路由路径，如 /user/list */
    private String path;

    /** 前端组件路径，如 views/user/UserList.vue */
    private String component;

    /** 图标 */
    private String icon;

    /** 权限标识，如 user:list（按钮级控制） */
    private String permission;

    /** 排序号，越小越靠前 */
    private Integer sort;

    /** 是否显示：1显示 0隐藏 */
    private Integer visible;

    /** 逻辑删除：0未删除 1已删除（由 yml 全局 logic-delete-field: deleted 生效） */
    private Integer deleted;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
