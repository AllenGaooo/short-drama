package com.bunny.flix.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bunny.flix.core.exception.BusinessException;
import com.bunny.flix.marketing.entity.UserCheckin;
import com.bunny.flix.marketing.mapper.UserCheckinMapper;
import com.bunny.flix.marketing.service.CheckinService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckinServiceImpl implements CheckinService {

    private final UserCheckinMapper userCheckinMapper;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int[] REWARDS = {5, 5, 10, 10, 15, 20, 30};

    @Override
    @Transactional
    public Map<String, Object> checkin(Long userId) {
        LocalDate today = LocalDate.now();
        Date todayDate = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant());

        // Check if already checked in today
        LambdaQueryWrapper<UserCheckin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCheckin::getUserId, userId)
               .eq(UserCheckin::getCheckinDate, todayDate);
        if (userCheckinMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("今日已签到");
        }

        // Get yesterday's checkin for consecutive count
        LocalDate yesterday = today.minusDays(1);
        Date yesterdayDate = Date.from(yesterday.atStartOfDay(ZoneId.systemDefault()).toInstant());

        LambdaQueryWrapper<UserCheckin> yesterdayWrapper = new LambdaQueryWrapper<>();
        yesterdayWrapper.eq(UserCheckin::getUserId, userId)
                        .eq(UserCheckin::getCheckinDate, yesterdayDate);
        UserCheckin yesterdayCheckin = userCheckinMapper.selectOne(yesterdayWrapper);

        int consecutiveDays = (yesterdayCheckin != null) ? yesterdayCheckin.getConsecutiveDays() + 1 : 1;
        if (consecutiveDays > 7) {
            consecutiveDays = 1;
        }

        UserCheckin checkin = new UserCheckin();
        checkin.setUserId(userId);
        checkin.setCheckinDate(todayDate);
        checkin.setConsecutiveDays(consecutiveDays);
        userCheckinMapper.insert(checkin);

        int rewardCoin = REWARDS[(consecutiveDays - 1) % 7];

        Map<String, Object> result = new HashMap<>();
        result.put("consecutiveDays", consecutiveDays);
        result.put("rewardCoin", rewardCoin);
        result.put("checkinDate", today.format(DTF));
        log.info("User {} checked in, consecutive days: {}, reward: {}", userId, consecutiveDays, rewardCoin);
        return result;
    }

    @Override
    public Map<String, Object> getCheckinCalendar(Long userId, String month) {
        YearMonth yearMonth = YearMonth.parse(month, DateTimeFormatter.ofPattern("yyyy-MM"));
        LocalDate startOfMonth = yearMonth.atDay(1);
        LocalDate endOfMonth = yearMonth.atEndOfMonth();

        Date start = Date.from(startOfMonth.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date end = Date.from(endOfMonth.atStartOfDay(ZoneId.systemDefault()).toInstant());

        LambdaQueryWrapper<UserCheckin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCheckin::getUserId, userId)
               .between(UserCheckin::getCheckinDate, start, end);
        List<UserCheckin> checkins = userCheckinMapper.selectList(wrapper);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        List<String> checkinDates = checkins.stream()
                .map(c -> sdf.format(c.getCheckinDate()))
                .toList();

        Map<String, Object> result = new HashMap<>();
        result.put("month", month);
        result.put("checkinDates", checkinDates);
        result.put("totalDays", checkinDates.size());
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> makeUpCheckin(Long userId, String date) {
        LocalDate targetDate = LocalDate.parse(date, DTF);
        Date target = Date.from(targetDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

        // Check if already checked in on this date
        LambdaQueryWrapper<UserCheckin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCheckin::getUserId, userId)
               .eq(UserCheckin::getCheckinDate, target);
        if (userCheckinMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("该日期已签到，无需补签");
        }

        // Cannot make up future dates
        if (targetDate.isAfter(LocalDate.now())) {
            throw new BusinessException("不能补签未来日期");
        }

        UserCheckin checkin = new UserCheckin();
        checkin.setUserId(userId);
        checkin.setCheckinDate(target);
        checkin.setConsecutiveDays(1);
        userCheckinMapper.insert(checkin);

        Map<String, Object> result = new HashMap<>();
        result.put("checkinDate", date);
        result.put("message", "补签成功");
        log.info("User {} made up checkin for date {}", userId, date);
        return result;
    }
}
