package com.campus.express.service;

import com.campus.express.event.IdleReportCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 上报提交后异步刷新拥挤度
 */
@Component
@RequiredArgsConstructor
public class CrowdScoreEventListener {

    private final CrowdScoreService crowdScoreService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(IdleReportCreatedEvent event) {
        crowdScoreService.recalculate(event.stationId());
    }
}
