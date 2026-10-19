package com.bunny.flix.feign.user.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 用户VIP信息 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserVipDTO {
    /** 用户ID */
    private Long userId;
    /** VIP状态：0-非会员，1-会员 */
    private Integer vipStatus;
    /** VIP到期时间 */
    private String vipExpireTime;
    /** 用户昵称 */
    private String nickname;
}
