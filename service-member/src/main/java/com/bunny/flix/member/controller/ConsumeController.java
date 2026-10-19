package com.bunny.flix.member.controller;

import com.bunny.flix.core.Result;
import com.bunny.flix.member.entity.ConsumeRecord;
import com.bunny.flix.member.service.ConsumeRecordService;
import com.bunny.flix.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/member/consume")
@RequiredArgsConstructor
public class ConsumeController {

    private final ConsumeRecordService consumeRecordService;

    @GetMapping("/records")
    public Result<List<ConsumeRecord>> getConsumeRecords(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer type) {
        Long userId = UserContext.getUserId();
        List<ConsumeRecord> records = consumeRecordService.getConsumeRecords(userId, page, size, type);
        return Result.success(records);
    }
}
