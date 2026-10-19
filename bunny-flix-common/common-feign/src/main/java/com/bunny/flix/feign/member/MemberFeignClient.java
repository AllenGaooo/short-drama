package com.bunny.flix.feign.member;

import com.bunny.flix.core.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 会员服务 Feign 客户端
 * 其他微服务通过这个接口调用 service-member 会员服务的 API
 */
@FeignClient(name = "service-member", path = "/api/member")
public interface MemberFeignClient {

    /** 给用户添加金币（比如完成任务奖励金币） */
    @PostMapping("/coin/add")
    Result<Void> addCoin(@RequestBody Map<String, Object> params);
}
