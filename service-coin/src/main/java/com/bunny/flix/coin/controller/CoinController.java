package com.bunny.flix.coin.controller;

import com.bunny.flix.coin.entity.CoinRecord;
import com.bunny.flix.coin.mapper.CoinRecordMapper;
import com.bunny.flix.coin.service.CoinService;
import com.bunny.flix.core.Result;
import com.bunny.flix.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 金币服务接口
 * 提供余额查询、流水查询，以及手动触发奖励（便于联调与测试）
 */
@Slf4j
@RestController
@RequestMapping("/api/coin")
@RequiredArgsConstructor
public class CoinController {

    private final CoinService coinService;
    private final CoinRecordMapper recordMapper;

    /** 查询我的金币余额与今日已获得 */
    @GetMapping("/balance")
    public Result<Map<String, Object>> balance() {
        Long userId = UserContext.getUserId();
        Map<String, Object> data = new HashMap<>();
        data.put("balance", coinService.getBalance(userId));
        data.put("todayEarned", coinService.getTodayEarned(userId));
        return Result.success(data);
    }

    /** 查询我的金币流水（最近 50 条） */
    @GetMapping("/records")
    public Result<List<CoinRecord>> records() {
        Long userId = UserContext.getUserId();
        List<CoinRecord> list = recordMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CoinRecord>()
                        .eq(CoinRecord::getUserId, userId)
                        .orderByDesc(CoinRecord::getCreateTime)
                        .last("LIMIT 50"));
        return Result.success(list);
    }

    /**
     * 手动触发看剧奖励（联调用）
     * 重复调用会被幂等拦截，可用于验证防重复发币
     */
    @PostMapping("/reward")
    public Result<Map<String, Object>> reward(@RequestParam Long episodeId,
                                              @RequestParam(required = false) Long dramaId) {
        Long userId = UserContext.getUserId();
        boolean granted = coinService.rewardForWatch(userId, episodeId, dramaId);
        Map<String, Object> data = new HashMap<>();
        data.put("granted", granted);
        data.put("message", granted ? "奖励发放成功" : "已发放过或超出限额，未重复发放");
        data.put("balance", coinService.getBalance(userId));
        return Result.success(data);
    }
}
