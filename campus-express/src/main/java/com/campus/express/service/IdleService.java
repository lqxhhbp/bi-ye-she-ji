package com.campus.express.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.express.common.exception.BusinessException;
import com.campus.express.common.util.CrowdLevelUtil;
import com.campus.express.dto.idle.IdleCalibrationRequest;
import com.campus.express.dto.idle.IdleReportRequest;
import com.campus.express.entity.IdleReport;
import com.campus.express.entity.Station;
import com.campus.express.event.IdleReportCreatedEvent;
import com.campus.express.mapper.IdleReportMapper;
import com.campus.express.mapper.StationMapper;
import com.campus.express.security.AuthUser;
import com.campus.express.security.UserContext;
import com.campus.express.vo.idle.CrowdSummaryVO;
import com.campus.express.vo.idle.IdleReportVO;
import com.campus.express.vo.idle.IdleTrendPointVO;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * 空闲度上报、校准和查询
 */
@Service
@RequiredArgsConstructor
public class IdleService {

    private final IdleReportMapper idleReportMapper;
    private final StationMapper stationMapper;
    private final AlertService alertService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(rollbackFor = Exception.class)
    public void report(IdleReportRequest request, String ip) {
        Station station = stationMapper.selectById(request.getStationId());
        if (station == null || station.getStatus() != 1) {
            throw BusinessException.notFound("站点不存在或已停用");
        }

        Long userId = UserContext.requireUserId();
        LocalDateTime now = LocalDateTime.now();
        Long recent = idleReportMapper.selectCount(Wrappers.<IdleReport>lambdaQuery()
                .eq(IdleReport::getUserId, userId)
                .eq(IdleReport::getStationId, request.getStationId())
                .ge(IdleReport::getCreatedAt, now.minusMinutes(10)));
        if (recent > 0) {
            throw new BusinessException(429, "同一站点 10 分钟内只能上报一次");
        }

        Long daily = idleReportMapper.selectCount(Wrappers.<IdleReport>lambdaQuery()
                .eq(IdleReport::getUserId, userId)
                .eq(IdleReport::getSource, 0)
                .ge(IdleReport::getCreatedAt, now.toLocalDate().atStartOfDay()));
        if (daily >= 5) {
            throw new BusinessException(429, "今日空闲度上报次数已达上限");
        }

        insertReport(request.getStationId(), userId, request.getLevel(), 0, ip);
        Long minuteCount = idleReportMapper.selectCount(Wrappers.<IdleReport>lambdaQuery()
                .eq(IdleReport::getStationId, request.getStationId())
                .eq(IdleReport::getSource, 0)
                .ge(IdleReport::getCreatedAt, now.truncatedTo(ChronoUnit.MINUTES)));
        if (minuteCount > 10) {
            alertService.createAbnormalIdleAlertIfNeeded(request.getStationId());
        }
        eventPublisher.publishEvent(new IdleReportCreatedEvent(request.getStationId()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void calibrate(IdleCalibrationRequest request, String ip) {
        AuthUser user = UserContext.require();
        if (user.stationId() == null) {
            throw BusinessException.forbidden("当前账号未绑定站点");
        }
        Station station = stationMapper.selectById(user.stationId());
        if (station == null || station.getStatus() != 1) {
            throw BusinessException.forbidden("当前站点不存在或已停用");
        }
        if (!addCalibrationIfAllowed(user.stationId(), request.getLevel(), ip, user.userId())) {
            throw new BusinessException(429, "本站点 10 分钟内已经校准，请稍后再试");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean addCalibrationIfAllowed(Long stationId, Integer level, String ip, Long userId) {
        Long recent = idleReportMapper.selectCount(Wrappers.<IdleReport>lambdaQuery()
                .eq(IdleReport::getStationId, stationId)
                .eq(IdleReport::getSource, 1)
                .ge(IdleReport::getCreatedAt, LocalDateTime.now().minusMinutes(10)));
        if (recent > 0) {
            return false;
        }
        insertReport(stationId, userId, level, 1, ip);
        eventPublisher.publishEvent(new IdleReportCreatedEvent(stationId));
        return true;
    }

    public List<IdleReportVO> recent(Long stationId) {
        Station station = requireStation(stationId);
        return idleReportMapper.selectList(Wrappers.<IdleReport>lambdaQuery()
                        .eq(IdleReport::getStationId, stationId)
                        .orderByDesc(IdleReport::getCreatedAt)
                        .last("LIMIT 20"))
                .stream()
                .map(report -> toReportVO(report, station.getName()))
                .toList();
    }

    public List<IdleTrendPointVO> trend(Long stationId, int hours) {
        if (hours < 1 || hours > 72) {
            throw BusinessException.badRequest("hours 必须在 1-72 之间");
        }
        requireStation(stationId);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime bucketStart = now.minusHours(hours).truncatedTo(ChronoUnit.MINUTES)
                .withMinute((now.minusHours(hours).getMinute() / 10) * 10)
                .withSecond(0)
                .withNano(0);
        List<IdleReport> reports = idleReportMapper.selectList(Wrappers.<IdleReport>lambdaQuery()
                .eq(IdleReport::getStationId, stationId)
                .ge(IdleReport::getCreatedAt, bucketStart)
                .orderByAsc(IdleReport::getCreatedAt));
        Map<LocalDateTime, List<IdleReport>> buckets = reports.stream()
                .collect(Collectors.groupingBy(
                        report -> report.getCreatedAt().truncatedTo(ChronoUnit.MINUTES)
                                .withMinute((report.getCreatedAt().getMinute() / 10) * 10),
                        TreeMap::new,
                        Collectors.toList()
                ));

        List<IdleTrendPointVO> result = new ArrayList<>();
        LocalDateTime point = bucketStart;
        while (!point.isAfter(now)) {
            List<IdleReport> bucketReports = buckets.getOrDefault(point, List.of());
            long count = bucketReports.size();
            double rawLevelAverage = bucketReports.stream()
                    .mapToInt(IdleReport::getLevel)
                    .average()
                    .orElse(0);
            double score = bucketReports.stream()
                    .mapToDouble(report -> CrowdLevelUtil.levelValue(report.getLevel()))
                    .average()
                    .orElse(0) * 100;
            result.add(new IdleTrendPointVO(
                    point,
                    BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.valueOf(rawLevelAverage).setScale(2, RoundingMode.HALF_UP),
                    count
            ));
            point = point.plusMinutes(10);
        }
        return result;
    }

    public List<CrowdSummaryVO> summary() {
        List<Station> stations = stationMapper.selectList(Wrappers.<Station>lambdaQuery()
                .eq(Station::getStatus, 1)
                .orderByAsc(Station::getId));
        LocalDateTime recentSince = LocalDateTime.now().minusMinutes(10);
        return stations.stream()
                .map(station -> CrowdSummaryVO.builder()
                        .stationId(station.getId())
                        .stationName(station.getName())
                        .crowdLevel(station.getCrowdLevel())
                        .crowdLevelName(CrowdLevelUtil.levelName(station.getCrowdLevel()))
                        .crowdScore(station.getCrowdScore())
                        .crowdUpdatedAt(station.getCrowdUpdatedAt())
                        .recentReportCount(idleReportMapper.selectCount(
                                Wrappers.<IdleReport>lambdaQuery()
                                        .eq(IdleReport::getStationId, station.getId())
                                        .ge(IdleReport::getCreatedAt, recentSince)))
                        .build())
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public int cleanupBefore(LocalDateTime cutoff) {
        return idleReportMapper.delete(Wrappers.<IdleReport>lambdaQuery()
                .lt(IdleReport::getCreatedAt, cutoff));
    }

    private void insertReport(Long stationId, Long userId, Integer level, int source, String ip) {
        IdleReport report = new IdleReport();
        report.setStationId(stationId);
        report.setUserId(userId);
        report.setLevel(level);
        report.setSource(source);
        report.setIp(ip);
        idleReportMapper.insert(report);
    }

    private Station requireStation(Long stationId) {
        Station station = stationMapper.selectById(stationId);
        if (station == null) {
            throw BusinessException.notFound("站点不存在");
        }
        return station;
    }

    private IdleReportVO toReportVO(IdleReport report, String stationName) {
        return IdleReportVO.builder()
                .id(report.getId())
                .stationId(report.getStationId())
                .stationName(stationName)
                .level(report.getLevel())
                .levelName(CrowdLevelUtil.levelName(report.getLevel()))
                .source(report.getSource())
                .createdAt(report.getCreatedAt())
                .build();
    }
}
