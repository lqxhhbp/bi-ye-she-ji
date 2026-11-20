package com.campus.express.task;

import com.campus.express.service.AlertService;
import com.campus.express.service.CrowdScoreService;
import com.campus.express.service.IdleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 平台定时任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledTasks {

    private final CrowdScoreService crowdScoreService;
    private final AlertService alertService;
    private final IdleService idleService;

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void runStartupMaintenance() {
        refreshCrowdScores();
        scanOverduePackages();
    }

    @Scheduled(cron = "0 */10 * * * *", zone = "Asia/Shanghai")
    public void refreshCrowdScores() {
        crowdScoreService.recalculateAll();
        log.info("拥挤度定时重算完成");
    }

    @Scheduled(cron = "30 */10 * * * *", zone = "Asia/Shanghai")
    public void scanOverduePackages() {
        int created = alertService.scanOverdueAlerts();
        if (created > 0) {
            log.info("超时包裹扫描完成，新增告警 {} 条", created);
        }
    }

    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Shanghai")
    public void cleanupIdleReports() {
        int deleted = idleService.cleanupBefore(LocalDateTime.now().minusDays(90));
        if (deleted > 0) {
            log.info("已清理 90 天前空闲度上报 {} 条", deleted);
        }
    }
}
