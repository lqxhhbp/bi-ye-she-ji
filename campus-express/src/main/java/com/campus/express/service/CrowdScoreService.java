package com.campus.express.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.express.common.util.CrowdLevelUtil;
import com.campus.express.entity.IdleReport;
import com.campus.express.entity.Station;
import com.campus.express.mapper.IdleReportMapper;
import com.campus.express.mapper.StationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 拥挤度指数计算
 */
@Service
@RequiredArgsConstructor
public class CrowdScoreService {

    private static final int WINDOW_HOURS = 2;

    private final IdleReportMapper idleReportMapper;
    private final StationMapper stationMapper;

    @Transactional(rollbackFor = Exception.class)
    public void recalculate(Long stationId) {
        LocalDateTime now = LocalDateTime.now();
        List<IdleReport> reports = idleReportMapper.selectList(Wrappers.<IdleReport>lambdaQuery()
                .eq(IdleReport::getStationId, stationId)
                .ge(IdleReport::getCreatedAt, now.minusHours(WINDOW_HOURS)));

        double score = calculateScore(reports, now);
        int level = CrowdLevelUtil.scoreToLevel(score);

        stationMapper.update(
                null,
                Wrappers.<Station>lambdaUpdate()
                        .eq(Station::getId, stationId)
                        .set(Station::getCrowdScore, BigDecimal.valueOf(score))
                        .set(Station::getCrowdLevel, level)
                        .set(Station::getCrowdUpdatedAt, now)
        );
    }

    double calculateScore(List<IdleReport> reports, LocalDateTime now) {
        List<IdleReport> filtered = filterHighFrequencyOutliers(reports);
        double weightedValue = 0;
        double weightSum = 0;
        for (IdleReport report : filtered) {
            double minutesAgo = Duration.between(report.getCreatedAt(), now).toSeconds() / 60.0;
            double weight = Math.pow(0.5, minutesAgo / 30.0);
            weightedValue += weight * CrowdLevelUtil.levelValue(report.getLevel());
            weightSum += weight;
        }
        double score = weightSum == 0 ? 0 : 100 * weightedValue / weightSum;
        return BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    @Transactional(rollbackFor = Exception.class)
    public void recalculateAll() {
        stationMapper.selectList(Wrappers.<Station>lambdaQuery()
                        .eq(Station::getStatus, 1))
                .forEach(station -> recalculate(station.getId()));
    }

    private List<IdleReport> filterHighFrequencyOutliers(List<IdleReport> reports) {
        Map<LocalDateTime, List<IdleReport>> byMinute = reports.stream()
                .filter(report -> report.getSource() == 0)
                .collect(Collectors.groupingBy(
                        report -> report.getCreatedAt().truncatedTo(ChronoUnit.MINUTES),
                        HashMap::new,
                        Collectors.toList()
                ));
        List<IdleReport> calibrations = reports.stream()
                .filter(report -> report.getSource() == 1)
                .toList();

        List<IdleReport> result = new ArrayList<>(calibrations);
        for (List<IdleReport> minuteReports : byMinute.values()) {
            if (minuteReports.size() <= 10) {
                result.addAll(minuteReports);
                continue;
            }
            List<Integer> levels = minuteReports.stream()
                    .map(IdleReport::getLevel)
                    .sorted()
                    .toList();
            double q1 = quantile(levels, 0.25);
            double q3 = quantile(levels, 0.75);
            double iqr = q3 - q1;
            double lower = q1 - 1.5 * iqr;
            double upper = q3 + 1.5 * iqr;
            result.addAll(minuteReports.stream()
                    .filter(report -> report.getLevel() >= lower && report.getLevel() <= upper)
                    .toList());
        }
        return result;
    }

    private double quantile(List<Integer> sortedValues, double percentile) {
        if (sortedValues.size() == 1) {
            return sortedValues.getFirst();
        }
        double position = percentile * (sortedValues.size() - 1);
        int lower = (int) Math.floor(position);
        int upper = (int) Math.ceil(position);
        if (lower == upper) {
            return sortedValues.get(lower);
        }
        double ratio = position - lower;
        return sortedValues.get(lower) * (1 - ratio) + sortedValues.get(upper) * ratio;
    }
}
