package com.bunny.flix.marketing.controller;

import com.bunny.flix.core.Result;
import com.bunny.flix.marketing.service.CheckinService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/marketing/checkin")
@RequiredArgsConstructor
public class CheckinController {

    private final CheckinService checkinService;

    @PostMapping
    public Result<Map<String, Object>> checkin(@RequestParam Long userId) {
        return Result.success(checkinService.checkin(userId));
    }

    @GetMapping("/calendar")
    public Result<Map<String, Object>> getCheckinCalendar(
            @RequestParam Long userId,
            @RequestParam String month) {
        return Result.success(checkinService.getCheckinCalendar(userId, month));
    }

    @PostMapping("/makeup")
    public Result<Map<String, Object>> makeUpCheckin(
            @RequestParam Long userId,
            @RequestParam String date) {
        return Result.success(checkinService.makeUpCheckin(userId, date));
    }
}
