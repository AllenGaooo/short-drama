package com.bunny.flix.data.scheduler;

import com.bunny.flix.data.service.RankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankingScheduler {

    private final RankingService rankingService;

    @Scheduled(cron = "0 */30 * * * ?")
    public void refreshRankings() {
        log.info("Scheduled ranking refresh started");
        try {
            rankingService.refreshRankings();
            log.info("Scheduled ranking refresh completed");
        } catch (Exception e) {
            log.error("Scheduled ranking refresh failed", e);
        }
    }
}
