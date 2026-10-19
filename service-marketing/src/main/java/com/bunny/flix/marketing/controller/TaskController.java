package com.bunny.flix.marketing.controller;

import com.bunny.flix.core.Result;
import com.bunny.flix.marketing.entity.Task;
import com.bunny.flix.marketing.entity.UserTask;
import com.bunny.flix.marketing.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/marketing/task")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    public Result<List<UserTask>> getUserTasks(@RequestParam Long userId) {
        return Result.success(taskService.getUserTasks(userId));
    }

    @PostMapping("/claim/{taskId}")
    public Result<Map<String, Object>> claimReward(
            @RequestParam Long userId,
            @PathVariable Long taskId) {
        return Result.success(taskService.claimReward(userId, taskId));
    }

    @PostMapping
    public Result<Task> createTask(@RequestBody Task task) {
        return Result.success(taskService.createTask(task));
    }
}
