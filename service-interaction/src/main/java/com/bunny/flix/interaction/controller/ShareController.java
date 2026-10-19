package com.bunny.flix.interaction.controller;

import com.bunny.flix.core.Result;
import com.bunny.flix.interaction.mq.InteractionEventProducer;
import com.bunny.flix.interaction.service.ShareService;
import com.bunny.flix.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/interaction/share")
@RequiredArgsConstructor
public class ShareController {

    private final ShareService shareService;
    private final InteractionEventProducer interactionEventProducer;

    @PostMapping
    public Result<Void> recordShare(@RequestBody Map<String, Object> body) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.unauthorized("请先登录");
        }

        Long dramaId = body.get("dramaId") != null ? Long.valueOf(body.get("dramaId").toString()) : null;
        String platform = (String) body.get("platform");

        shareService.recordShare(userId, dramaId, platform);
        interactionEventProducer.sendInteractionEvent("SHARE", userId, dramaId, null);
        return Result.success();
    }
}
