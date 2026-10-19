package com.bunny.flix.feign.user;

import com.bunny.flix.core.Result;
import com.bunny.flix.feign.user.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * 用户服务 Feign 客户端
 * 其他微服务通过这个接口调用 service-user 用户服务的 API
 */
@FeignClient(name = "service-user", path = "/api/user")
public interface UserFeignClient {

    /** 根据用户ID查询用户信息 */
    @GetMapping("/{userId}")
    Result<UserDTO> getUserById(@PathVariable("userId") Long userId);

    /** 根据手机号查询用户信息 */
    @GetMapping("/phone/{phone}")
    Result<UserDTO> getUserByPhone(@PathVariable("phone") String phone);

    /** 更新用户的VIP状态（会员到期时调用） */
    @PutMapping("/vip/status")
    Result<Void> updateVipStatus(@RequestBody VipStatusDTO dto);

    /** 查询用户的VIP信息 */
    @GetMapping("/{userId}/vip")
    Result<UserVipDTO> getUserVipInfo(@PathVariable("userId") Long userId);

    /** 判断用户是否存在 */
    @GetMapping("/{userId}/exists")
    Result<Boolean> userExists(@PathVariable("userId") Long userId);
}
