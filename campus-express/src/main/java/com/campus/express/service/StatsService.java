package com.campus.express.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.express.common.exception.BusinessException;
import com.campus.express.common.util.CrowdLevelUtil;
import com.campus.express.entity.ExpressPackage;
import com.campus.express.entity.PackageLog;
import com.campus.express.entity.Station;
import com.campus.express.entity.SysUser;
import com.campus.express.mapper.ExpressPackageMapper;
import com.campus.express.mapper.PackageLogMapper;
import com.campus.express.mapper.StationMapper;
import com.campus.express.mapper.SysUserMapper;
import com.campus.express.security.AuthUser;
import com.campus.express.security.UserContext;
import com.campus.express.vo.stats.CrowdDistributionVO;
import com.campus.express.vo.stats.OperatorWorkloadVO;
import com.campus.express.vo.stats.StationDailyVO;
import com.campus.express.vo.stats.StationThroughputVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 统计业务
 */
@Service
@RequiredArgsConstructor
public class StatsService {

    private final ExpressPackageMapper packageMapper;
    private final PackageLogMapper packageLogMapper;
    private final StationMapper stationMapper;
    private final SysUserMapper userMapper;

    public List<StationThroughputVO> stationThroughput(int days) {
        LocalDateTime start = rangeStart(days);
        List<Station> stations = stationMapper.selectList(Wrappers.<Station>lambdaQuery()
                .eq(Station::getStatus, 1));
        List<ExpressPackage> packages = packageMapper.selectList(
                Wrappers.<ExpressPackage>lambdaQuery()
                        .and(wrapper -> wrapper
                                .ge(ExpressPackage::getInboundAt, start)
                                .or()
                                .ge(ExpressPackage::getTakenAt, start)));
        LocalDateTime overdueBefore = LocalDateTime.now().minusHours(48);
        List<ExpressPackage> overduePackages = packageMapper.selectList(
                Wrappers.<ExpressPackage>lambdaQuery()
                        .eq(ExpressPackage::getStatus, 0)
                        .lt(ExpressPackage::getInboundAt, overdueBefore));

        Map<Long, Long> inbound = packages.stream()
                .collect(Collectors.groupingBy(ExpressPackage::getStationId, Collectors.counting()));
        Map<Long, Long> taken = packages.stream()
                .filter(item -> item.getStatus() == 1
                        && item.getTakenAt() != null
                        && !item.getTakenAt().isBefore(start))
                .collect(Collectors.groupingBy(ExpressPackage::getStationId, Collectors.counting()));
        Map<Long, Long> overdue = overduePackages.stream()
                .collect(Collectors.groupingBy(ExpressPackage::getStationId, Collectors.counting()));

        return stations.stream()
                .map(station -> StationThroughputVO.builder()
                        .stationId(station.getId())
                        .stationName(station.getName())
                        .inboundCount(inbound.getOrDefault(station.getId(), 0L))
                        .takenCount(taken.getOrDefault(station.getId(), 0L))
                        .overdueCount(overdue.getOrDefault(station.getId(), 0L))
                        .build())
                .sorted(Comparator.comparing(StationThroughputVO::getInboundCount).reversed())
                .toList();
    }

    public List<StationDailyVO> stationDaily(int days) {
        LocalDateTime start = rangeStart(days);
        Long stationId = currentStationId();
        List<ExpressPackage> packages = packageMapper.selectList(Wrappers.<ExpressPackage>lambdaQuery()
                .eq(ExpressPackage::getStationId, stationId)
                .and(wrapper -> wrapper
                        .ge(ExpressPackage::getInboundAt, start)
                        .or()
                        .ge(ExpressPackage::getTakenAt, start)));

        Map<LocalDate, Long> inbound = new HashMap<>();
        Map<LocalDate, Long> taken = new HashMap<>();
        Map<LocalDate, Long> revoked = new HashMap<>();
        for (ExpressPackage item : packages) {
            if (!item.getInboundAt().isBefore(start)) {
                LocalDate inboundDate = item.getInboundAt().toLocalDate();
                inbound.merge(inboundDate, 1L, Long::sum);
                if (item.getStatus() == 2) {
                    revoked.merge(inboundDate, 1L, Long::sum);
                }
            }
            if (item.getTakenAt() != null && !item.getTakenAt().isBefore(start)) {
                taken.merge(item.getTakenAt().toLocalDate(), 1L, Long::sum);
            }
        }

        List<StationDailyVO> result = new ArrayList<>();
        LocalDate date = start.toLocalDate();
        LocalDate today = LocalDate.now();
        while (!date.isAfter(today)) {
            result.add(new StationDailyVO(
                    date,
                    inbound.getOrDefault(date, 0L),
                    taken.getOrDefault(date, 0L),
                    revoked.getOrDefault(date, 0L)
            ));
            date = date.plusDays(1);
        }
        return result;
    }

    public List<OperatorWorkloadVO> operatorWorkload(int days) {
        LocalDateTime start = rangeStart(days);
        Map<Long, Long> inbound = packageMapper.selectList(Wrappers.<ExpressPackage>lambdaQuery()
                        .ge(ExpressPackage::getInboundAt, start))
                .stream()
                .collect(Collectors.groupingBy(ExpressPackage::getOperatorId, Collectors.counting()));
        Map<Long, Long> verified = packageLogMapper.selectList(Wrappers.<PackageLog>lambdaQuery()
                        .eq(PackageLog::getAction, 2)
                        .ge(PackageLog::getCreatedAt, start))
                .stream()
                .collect(Collectors.groupingBy(PackageLog::getOperatorId, Collectors.counting()));

        Set<Long> userIds = new HashSet<>(inbound.keySet());
        userIds.addAll(verified.keySet());
        if (userIds.isEmpty()) {
            return List.of();
        }
        Map<Long, SysUser> users = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));
        Set<Long> stationIds = users.values().stream()
                .map(SysUser::getStationId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> stationNames = stationIds.isEmpty()
                ? Map.of()
                : stationMapper.selectBatchIds(stationIds).stream()
                        .collect(Collectors.toMap(Station::getId, Station::getName));

        return userIds.stream().map(userId -> {
            SysUser user = users.get(userId);
            return OperatorWorkloadVO.builder()
                    .userId(userId)
                    .realName(user == null ? "未知用户" : user.getRealName())
                    .stationName(user == null || user.getStationId() == null
                            ? null
                            : stationNames.get(user.getStationId()))
                    .inboundCount(inbound.getOrDefault(userId, 0L))
                    .verifiedCount(verified.getOrDefault(userId, 0L))
                    .build();
        }).sorted(Comparator
                        .comparing(OperatorWorkloadVO::getInboundCount).reversed()
                        .thenComparing(OperatorWorkloadVO::getVerifiedCount, Comparator.reverseOrder()))
                .toList();
    }

    public List<CrowdDistributionVO> crowdDistribution() {
        Map<Integer, Long> counts = stationMapper.selectList(Wrappers.<Station>lambdaQuery()
                        .eq(Station::getStatus, 1))
                .stream()
                .collect(Collectors.groupingBy(Station::getCrowdLevel, Collectors.counting()));
        List<CrowdDistributionVO> result = new ArrayList<>();
        for (int level = 0; level <= 2; level++) {
            result.add(new CrowdDistributionVO(
                    level,
                    CrowdLevelUtil.levelName(level),
                    counts.getOrDefault(level, 0L)
            ));
        }
        return result;
    }

    private LocalDateTime rangeStart(int days) {
        if (days < 1 || days > 90) {
            throw BusinessException.badRequest("days 必须在 1-90 之间");
        }
        return LocalDate.now().minusDays(days - 1L).atStartOfDay();
    }

    private Long currentStationId() {
        AuthUser user = UserContext.require();
        if (user.stationId() == null) {
            throw BusinessException.forbidden("当前账号未绑定站点");
        }
        return user.stationId();
    }
}
