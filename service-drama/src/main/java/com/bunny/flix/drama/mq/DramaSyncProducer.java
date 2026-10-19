package com.bunny.flix.drama.mq;

import com.bunny.flix.feign.drama.dto.DramaDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DramaSyncProducer {

    private static final String TOPIC = "drama-sync";
    private final RocketMQTemplate rocketMQTemplate;

    /**
     * 发送短剧同步消息（同步到搜索服务更新 ES 索引）
     *
     * ⚠️ 必须做异常降级：
     *    MQ 属于"旁路"，不是主流程依赖。若 NameServer 不可用导致这里抛异常，
     *    会让 createDrama / updateDrama / deleteDrama 全部失败——
     *    即"搜索索引同步故障"拖垮"短剧核心写入"，这是不可接受的。
     *    因此发送失败只记录日志，由定时任务全量重建索引兜底最终一致。
     */
    public void sendDramaSync(DramaDTO dramaDTO) {
        try {
            rocketMQTemplate.convertAndSend(TOPIC, dramaDTO);
            log.info("发送短剧同步消息成功: dramaId={}", dramaDTO.getId());
        } catch (Exception e) {
            log.error("发送短剧同步消息失败 dramaId={}，索引同步将延迟（由全量重建任务兜底）: {}",
                    dramaDTO.getId(), e.getMessage());
        }
    }
}
