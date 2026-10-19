package com.bunny.flix.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体类，对应数据库 t_user 表
 * 存储用户的基本信息、VIP状态等
 */
@Data
@TableName("t_user")
public class User {

    /** 用户ID，由 service-id-generator 号段模式分配 */
    @TableId(type = IdType.INPUT)
    private Long id;

    /** 手机号，用于登录 */
    private String phone;

    /** 密码（加密后的） */
    private String password;

    /** 昵称 */
    private String nickname;

    /** 头像URL */
    private String avatar;

    /** 性别：0-未知，1-男，2-女 */
    private Integer gender;

    /** 生日 */
    private String birthday;

    /** 个性签名 */
    private String signature;

    /** 密码盐值，用于加密 */
    private String salt;

    /** 账号状态：0-正常，1-冻结 */
    private Integer status;

    /** VIP状态：0-非会员，1-会员 */
    private Integer vipStatus;

    /** VIP到期时间 */
    private LocalDateTime vipExpireTime;

    /** 注册时间 */
    private LocalDateTime createTime;

    /** 最后更新时间 */
    private LocalDateTime updateTime;
}
