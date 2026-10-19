package com.bunny.flix.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bunny.flix.core.exception.BusinessException;
import com.bunny.flix.feign.member.MemberFeignClient;
import com.bunny.flix.marketing.entity.Task;
import com.bunny.flix.marketing.entity.UserTask;
import com.bunny.flix.marketing.mapper.TaskMapper;
import com.bunny.flix.marketing.mapper.UserTaskMapper;
import com.bunny.flix.marketing.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final UserTaskMapper userTaskMapper;
    private final TaskMapper taskMapper;
    private final MemberFeignClient memberFeignClient;

    @Override
    public List<UserTask> getUserTasks(Long userId) {
        LambdaQueryWrapper<UserTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserTask::getUserId, userId);
        return userTaskMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public void updateProgress(Long userId, Long taskId, Integer progress) {
        LambdaQueryWrapper<UserTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserTask::getUserId, userId)
               .eq(UserTask::getTaskId, taskId);

        UserTask userTask = userTaskMapper.selectOne(wrapper);
        if (userTask == null) {
            Task task = taskMapper.selectById(taskId);
            if (task == null || task.getEnabled() == 0) {
                throw new BusinessException("任务不存在或已禁用");
            }

            userTask = new UserTask();
            userTask.setUserId(userId);
            userTask.setTaskId(taskId);
            userTask.setStatus(0);
            userTask.setProgress(progress);
            userTaskMapper.insert(userTask);
        } else {
            userTask.setProgress(progress);
            userTaskMapper.updateById(userTask);
        }
        log.info("Updated task progress: userId={}, taskId={}, progress={}", userId, taskId, progress);
    }

    @Override
    @Transactional
    public Map<String, Object> claimReward(Long userId, Long taskId) {
        LambdaQueryWrapper<UserTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserTask::getUserId, userId)
               .eq(UserTask::getTaskId, taskId);
        UserTask userTask = userTaskMapper.selectOne(wrapper);

        if (userTask == null) {
            throw new BusinessException(BusinessException.ResultCode.TASK_NOT_COMPLETED);
        }
        if (userTask.getStatus() == 2) {
            throw new BusinessException(BusinessException.ResultCode.TASK_REWARD_CLAIMED);
        }
        if (userTask.getStatus() != 1) {
            throw new BusinessException(BusinessException.ResultCode.TASK_NOT_COMPLETED);
        }

        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }

        // Claim reward - mark as claimed
        userTask.setStatus(2);
        userTaskMapper.updateById(userTask);

        // Call member service to add coins
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("userId", userId);
            params.put("amount", task.getRewardCoin());
            params.put("reason", "任务奖励: " + task.getName());
            memberFeignClient.addCoin(params);
        } catch (Exception e) {
            log.error("Failed to add coins via member service: userId={}, taskId={}", userId, taskId, e);
            // In production, you'd want to handle this with a retry or compensation mechanism
        }

        Map<String, Object> result = new HashMap<>();
        result.put("taskId", taskId);
        result.put("rewardCoin", task.getRewardCoin());
        result.put("message", "领取成功");
        log.info("User {} claimed reward for task {}, coins: {}", userId, taskId, task.getRewardCoin());
        return result;
    }

    @Override
    public Task createTask(Task task) {
        taskMapper.insert(task);
        log.info("Created task: {}", task.getName());
        return task;
    }
}
