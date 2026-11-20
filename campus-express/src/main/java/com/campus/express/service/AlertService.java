package com.campus.express.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.express.common.PageResult;
import com.campus.express.common.exception.BusinessException;
import com.campus.express.entity.AlertRecord;
import com.campus.express.entity.ExpressPackage;
import com.campus.express.entity.Station;
import com.campus.express.mapper.AlertRecordMapper;
import com.campus.express.mapper.ExpressPackageMapper;
import com.campus.express.mapper.StationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 告警生成与处理
 */
@Service
@RequiredArgsConstructor
public class AlertService {

    private static final int TIMEOUT_TYPE = 0;
    private static final int ABNORMAL_IDLE_TYPE = 1;
    private static final int STATION_DISABLED_TYPE = 2;

    private final AlertRecordMapper alertRecordMapper;
    private final ExpressPackageMapper packageMapper;
    private final StationMapper stationMapper;

    @Transactional(rollbackFor = Exception.class)
    public int scanOverdueAlerts() {
        LocalDateTime deadline = LocalDateTime.now().minusHours(48);
        List<ExpressPackage> packages = packageMapper.selectList(Wrappers.<ExpressPackage>lambdaQuery()
                .eq(ExpressPackage::getStatus, 0)
                .lt(ExpressPackage::getInboundAt, deadline));
        int created = 0;
        for (ExpressPackage expressPackage : packages) {
            Long count = alertRecordMapper.selectCount(Wrappers.<AlertRecord>lambdaQuery()
                    .eq(AlertRecord::getType, TIMEOUT_TYPE)
                    .eq(AlertRecord::getPackageId, expressPackage.getId()));
            if (count > 0) {
                continue;
            }
            AlertRecord alert = new AlertRecord();
            alert.setType(TIMEOUT_TYPE);
            alert.setStationId(expressPackage.getStationId());
            alert.setPackageId(expressPackage.getId());
            alert.setContent("包裹 " + expressPackage.getPickupCode() + " 待取超过 48 小时");
            alert.setStatus(0);
            try {
                alertRecordMapper.insert(alert);
                created++;
            } catch (DuplicateKeyException ignored) {
                // 唯一键兜底：定时任务并发执行时不重复插入。
            }
        }
        return created;
    }

    public void closeTimeoutAlert(Long packageId) {
        alertRecordMapper.update(
                null,
                Wrappers.<AlertRecord>lambdaUpdate()
                        .eq(AlertRecord::getType, TIMEOUT_TYPE)
                        .eq(AlertRecord::getPackageId, packageId)
                        .eq(AlertRecord::getStatus, 0)
                        .set(AlertRecord::getStatus, 1)
                        .set(AlertRecord::getHandledAt, LocalDateTime.now())
        );
    }

    public void createStationDisabledAlert(Long stationId) {
        Station station = stationMapper.selectById(stationId);
        if (station == null) {
            return;
        }
        Long recent = alertRecordMapper.selectCount(Wrappers.<AlertRecord>lambdaQuery()
                .eq(AlertRecord::getType, STATION_DISABLED_TYPE)
                .eq(AlertRecord::getStationId, stationId)
                .eq(AlertRecord::getStatus, 0));
        if (recent > 0) {
            return;
        }
        AlertRecord alert = new AlertRecord();
        alert.setType(STATION_DISABLED_TYPE);
        alert.setStationId(stationId);
        alert.setContent("站点已停用：" + station.getName());
        alert.setStatus(0);
        alertRecordMapper.insert(alert);
    }

    public void createAbnormalIdleAlertIfNeeded(Long stationId) {
        Long recent = alertRecordMapper.selectCount(Wrappers.<AlertRecord>lambdaQuery()
                .eq(AlertRecord::getType, ABNORMAL_IDLE_TYPE)
                .eq(AlertRecord::getStationId, stationId)
                .ge(AlertRecord::getCreatedAt, LocalDateTime.now().minusHours(1)));
        if (recent > 0) {
            return;
        }
        Station station = stationMapper.selectById(stationId);
        if (station == null) {
            return;
        }
        AlertRecord alert = new AlertRecord();
        alert.setType(ABNORMAL_IDLE_TYPE);
        alert.setStationId(stationId);
        alert.setContent(station.getName() + " 1 分钟内空闲度上报超过 10 条，已启用异常值过滤");
        alert.setStatus(0);
        alertRecordMapper.insert(alert);
    }

    public PageResult<AlertRecord> list(int page, int pageSize, Integer status, Integer type) {
        Page<AlertRecord> result = alertRecordMapper.selectPage(
                new Page<>(Math.max(page, 1), Math.min(Math.max(pageSize, 1), 100)),
                Wrappers.<AlertRecord>lambdaQuery()
                        .eq(status != null, AlertRecord::getStatus, status)
                        .eq(type != null, AlertRecord::getType, type)
                        .orderByDesc(AlertRecord::getCreatedAt)
        );
        return new PageResult<>(result.getTotal(), result.getRecords());
    }

    @Transactional(rollbackFor = Exception.class)
    public void handle(Long id) {
        int updated = alertRecordMapper.update(
                null,
                Wrappers.<AlertRecord>lambdaUpdate()
                        .eq(AlertRecord::getId, id)
                        .eq(AlertRecord::getStatus, 0)
                        .set(AlertRecord::getStatus, 1)
                        .set(AlertRecord::getHandledAt, LocalDateTime.now())
        );
        if (updated == 0) {
            throw BusinessException.badRequest("告警不存在或已处理");
        }
    }
}
