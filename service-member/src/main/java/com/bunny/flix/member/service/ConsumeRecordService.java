package com.bunny.flix.member.service;

import com.bunny.flix.member.entity.ConsumeRecord;

import java.util.List;

public interface ConsumeRecordService {

    void recordConsume(Long userId, Long amount, Long dramaId, String description);

    List<ConsumeRecord> getConsumeRecords(Long userId, Integer page, Integer size, Integer type);
}
