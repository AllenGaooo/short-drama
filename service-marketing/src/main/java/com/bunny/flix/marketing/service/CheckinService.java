package com.bunny.flix.marketing.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bunny.flix.core.exception.BusinessException;
import com.bunny.flix.marketing.entity.UserCheckin;
import com.bunny.flix.marketing.mapper.UserCheckinMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CheckinService {

    private final UserCheckinMapper checkinMapper;

    public int checkin(Long userId) {
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<UserCheckin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCheckin::getUserId, userId).eq(UserCheckin::getCheckinDate, today);
        if (checkinMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("今日已签到");
        }

        // 查询昨天的签到记录
        LocalDate yesterday = today.minusDays(1);
        LambdaQueryWrapper<UserCheckin> yw = new LambdaQueryWrapper<>();
        yw.eq(UserCheckin::getUserId, userId).eq(UserCheckin::getCheckinDate, yesterday);
        UserCheckin yesterdayCheckin = checkinMapper.selectOne(yw);

        int consecutiveDays = yesterdayCheckin != null ? yesterdayCheckin.getConsecutiveDays() + 1 : 1;

        UserCheckin checkin = new UserCheckin();
        checkin.setUserId(userId);
        checkin.setCheckinDate(today);
        checkin.setConsecutiveDays(consecutiveDays);
        checkinMapper.insert(checkin);

        // 连续7天奖励递增
        int reward = Math.min(consecutiveDays, 7) * 5;
        return reward;
    }

    public List<UserCheckin> getCheckinCalendar(Long userId, int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1);
        LambdaQueryWrapper<UserCheckin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCheckin::getUserId, userId)
               .ge(UserCheckin::getCheckinDate, start)
               .lt(UserCheckin::getCheckinDate, end)
               .orderByAsc(UserCheckin::getCheckinDate);
        return checkinMapper.selectList(wrapper);
    }
}
