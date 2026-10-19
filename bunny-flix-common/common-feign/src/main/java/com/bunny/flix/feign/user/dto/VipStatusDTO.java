package com.bunny.flix.feign.user.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 更新VIP状态请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VipStatusDTO {
    /** 用户ID */
    private Long userId;
    /** VIP状态 */
    private Integer vipStatus;
    /** VIP到期时间 */
    private String vipExpireTime;
}
