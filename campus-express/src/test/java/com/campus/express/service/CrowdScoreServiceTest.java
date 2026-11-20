package com.campus.express.service;

import com.campus.express.entity.IdleReport;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CrowdScoreServiceTest {

    private final CrowdScoreService service = new CrowdScoreService(null, null);
    private final LocalDateTime now = LocalDateTime.of(2026, 11, 19, 12, 0);

    @Test
    void shouldReturnZeroWithoutReports() {
        assertEquals(0, service.calculateScore(List.of(), now));
    }

    @Test
    void shouldApplyThirtyMinuteHalfLife() {
        List<IdleReport> reports = List.of(
                report(2, 0, 0),
                report(0, 30, 0)
        );

        assertEquals(63.33, service.calculateScore(reports, now));
    }

    @Test
    void shouldFilterOutliersWhenMinuteHasMoreThanTenReports() {
        List<IdleReport> reports = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            reports.add(report(0, 0, 0));
        }
        reports.add(report(2, 0, 0));

        assertEquals(10, service.calculateScore(reports, now));
    }

    private IdleReport report(int level, int minutesAgo, int source) {
        IdleReport report = new IdleReport();
        report.setLevel(level);
        report.setSource(source);
        report.setCreatedAt(now.minusMinutes(minutesAgo));
        return report;
    }
}
