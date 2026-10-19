package com.bunny.flix.feign.idgenerator;

import com.bunny.flix.core.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ID生成器服务 Feign 客户端
 * 分布式ID生成，使用号段模式（类似美团Leaf），各个业务类型独立分配ID。
 * 调用方通过 {@link BizTypeEnum} 声明业务，号段由服务内部判断，不传裸字符串。
 */
@FeignClient(name = "service-id-generator", path = "/api/idgen")
public interface IdGeneratorFeignClient {

    /** 获取下一个ID（根据业务枚举类型判断号段，如 USER、ORDER、DRAMA） */
    @GetMapping("/next")
    Result<Long> nextId(@RequestParam("bizType") BizTypeEnum bizType);

    /** 批量获取ID（一次取多个，减少请求次数） */
    @PostMapping("/batch")
    Result<List<Long>> batchIds(@RequestParam("bizType") BizTypeEnum bizType,
                                @RequestParam("count") Integer count);
}
