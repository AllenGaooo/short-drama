package com.bunny.flix.feign.user.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 用户信息 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long id;
    /** 手机号 */
    private String phone;
    /** 昵称 */
    private String nickname;
    /** 头像地址 */
    private String avatar;
    /** 性别：0-未知，1-男，2-女 */
    private Integer gender;
    /** 生日 */
    private String birthday;
    /** 个性签名 */
    private String signature;
    /** 状态：0-正常，1-冻结 */
    private Integer status;
    /** 注册时间 */
    private String createTime;
}
