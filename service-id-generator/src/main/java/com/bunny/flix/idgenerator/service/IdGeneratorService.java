package com.bunny.flix.idgenerator.service;

import com.bunny.flix.feign.idgenerator.BizTypeEnum;
import com.bunny.flix.idgenerator.service.SegmentBuffer.SegmentStatus;

import java.util.List;
import java.util.Map;

public interface IdGeneratorService {

    /** 根据业务枚举获取下一个ID（号段由枚举内部映射） */
    Long getId(BizTypeEnum bizType);

    /** 根据业务枚举批量获取ID */
    List<Long> batchGetIds(BizTypeEnum bizType, Integer count);

    /** 预热所有号段 */
    void warmUpAll();

    /** 各 bizTag 号段状态（监控用） */
    List<SegmentStatus> status();

    /** 整体指标：QPS、平均耗时（监控用） */
    Map<String, Object> metrics();
}
