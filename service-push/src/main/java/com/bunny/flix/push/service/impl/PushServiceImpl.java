package com.bunny.flix.push.service.impl;

import com.bunny.flix.push.entity.PushRecord;
import com.bunny.flix.push.mapper.PushRecordMapper;
import com.bunny.flix.push.service.PushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushServiceImpl implements PushService {

    private final PushRecordMapper pushRecordMapper;

    @Override
    public void pushToApp(Long userId, String title, String content) {
        // Simplified implementation: in production this would integrate with
        // push platforms like JPush (极光推送) or Getui (个推)
        PushRecord record = new PushRecord();
        record.setUserId(userId);
        record.setTitle(title);
        record.setContent(content);
        record.setPushType("app");
        record.setPushTime(LocalDateTime.now());
        record.setResult("success");
        pushRecordMapper.insert(record);
        log.info("App push to user {}: {}", userId, title);
    }

    @Override
    public void pushNewEpisode(Long userId, Long dramaId, String episodeTitle) {
        String title = "追剧更新";
        String content = "您追的短剧更新了新剧集：" + episodeTitle;

        PushRecord record = new PushRecord();
        record.setUserId(userId);
        record.setTitle(title);
        record.setContent(content);
        record.setPushType("app");
        record.setPushTime(LocalDateTime.now());
        record.setResult("success");
        pushRecordMapper.insert(record);
        log.info("New episode push to user {}: dramaId={}, episode={}", userId, dramaId, episodeTitle);
    }
}
