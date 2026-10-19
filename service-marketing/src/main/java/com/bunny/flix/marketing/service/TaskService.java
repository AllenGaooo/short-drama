package com.bunny.flix.marketing.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bunny.flix.core.exception.BusinessException;
import com.bunny.flix.marketing.entity.Task;
import com.bunny.flix.marketing.entity.UserTask;
import com.bunny.flix.marketing.mapper.TaskMapper;
import com.bunny.flix.marketing.mapper.UserTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskMapper taskMapper;
    private final UserTaskMapper userTaskMapper;

    public List<Task> getEnabledTasks() {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Task::getEnabled, 1);
        return taskMapper.selectList(wrapper);
    }

    public List<UserTask> getUserTasks(Long userId) {
        LambdaQueryWrapper<UserTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserTask::getUserId, userId);
        return userTaskMapper.selectList(wrapper);
    }

    public void updateProgress(Long userId, Long taskId, int progress) {
        LambdaQueryWrapper<UserTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserTask::getUserId, userId).eq(UserTask::getTaskId, taskId);
        UserTask userTask = userTaskMapper.selectOne(wrapper);
        if (userTask != null) {
            userTask.setProgress(progress);
            Task task = taskMapper.selectById(taskId);
            if (task != null && task.getDurationRequired() != null && progress >= task.getDurationRequired()) {
                userTask.setStatus(1); // 已完成
            }
            userTaskMapper.updateById(userTask);
        }
    }

    @Transactional
    public int claimReward(Long userId, Long taskId) {
        LambdaQueryWrapper<UserTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserTask::getUserId, userId).eq(UserTask::getTaskId, taskId);
        UserTask userTask = userTaskMapper.selectOne(wrapper);
        if (userTask == null || userTask.getStatus() != 1) {
            throw new BusinessException("任务未完成或奖励已领取");
        }

        Task task = taskMapper.selectById(taskId);
        if (task == null || task.getEnabled() == 0) {
            throw new BusinessException("任务不存在");
        }

        userTask.setStatus(2); // 已领取
        userTaskMapper.updateById(userTask);

        return task.getRewardCoin();
    }

    public Task createTask(Task task) {
        taskMapper.insert(task);
        return task;
    }
}
