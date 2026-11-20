package com.campus.express.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.express.common.PageResult;
import com.campus.express.common.exception.BusinessException;
import com.campus.express.common.util.PhoneMaskUtil;
import com.campus.express.dto.audit.AuditReviewRequest;
import com.campus.express.entity.Station;
import com.campus.express.entity.StationAudit;
import com.campus.express.entity.SysUser;
import com.campus.express.mapper.StationAuditMapper;
import com.campus.express.mapper.StationMapper;
import com.campus.express.mapper.SysUserMapper;
import com.campus.express.security.UserContext;
import com.campus.express.vo.audit.AuditVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 站长入驻审核业务
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final StationAuditMapper auditMapper;
    private final SysUserMapper userMapper;
    private final StationMapper stationMapper;

    public PageResult<AuditVO> list(int page, int pageSize, Integer status) {
        Page<StationAudit> result = auditMapper.selectPage(
                new Page<>(Math.max(page, 1), Math.min(Math.max(pageSize, 1), 100)),
                Wrappers.<StationAudit>lambdaQuery()
                        .eq(status != null, StationAudit::getStatus, status)
                        .orderByDesc(StationAudit::getCreatedAt)
        );
        return new PageResult<>(result.getTotal(), toAuditVOs(result.getRecords()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void review(Long id, AuditReviewRequest request) {
        StationAudit audit = auditMapper.selectById(id);
        if (audit == null || audit.getStatus() != 0) {
            throw BusinessException.badRequest("申请不存在或已审核");
        }

        Long reviewerId = UserContext.requireUserId();
        LocalDateTime reviewTime = LocalDateTime.now();
        int updated = auditMapper.update(
                null,
                Wrappers.<StationAudit>lambdaUpdate()
                        .eq(StationAudit::getId, id)
                        .eq(StationAudit::getStatus, 0)
                        .set(StationAudit::getStatus, request.getStatus())
                        .set(StationAudit::getReviewerId, reviewerId)
                        .set(StationAudit::getReviewTime, reviewTime)
        );
        if (updated == 0) {
            throw BusinessException.badRequest("申请已被其他管理员处理");
        }

        if (request.getStatus() == 1) {
            SysUser user = userMapper.selectById(audit.getUserId());
            if (user == null) {
                throw BusinessException.notFound("申请用户不存在");
            }
            SysUser update = new SysUser();
            update.setId(user.getId());
            update.setRole(1);
            update.setStationId(audit.getStationId());
            update.setStatus(1);
            userMapper.updateById(update);
        }
    }

    private List<AuditVO> toAuditVOs(List<StationAudit> audits) {
        if (audits.isEmpty()) {
            return List.of();
        }
        Set<Long> userIds = audits.stream()
                .flatMap(audit -> java.util.stream.Stream.of(audit.getUserId(), audit.getReviewerId()))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, SysUser> users = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));
        Map<Long, Station> stations = stationMapper.selectBatchIds(
                        audits.stream().map(StationAudit::getStationId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Station::getId, Function.identity()));

        return audits.stream().map(audit -> {
            SysUser applicant = users.get(audit.getUserId());
            Station station = stations.get(audit.getStationId());
            return AuditVO.builder()
                    .id(audit.getId())
                    .userId(audit.getUserId())
                    .applicantName(applicant == null ? null : applicant.getRealName())
                    .applicantPhone(applicant == null ? null : PhoneMaskUtil.mask(applicant.getPhone()))
                    .stationId(audit.getStationId())
                    .stationName(station == null ? null : station.getName())
                    .applyReason(audit.getApplyReason())
                    .status(audit.getStatus())
                    .reviewerId(audit.getReviewerId())
                    .reviewTime(audit.getReviewTime())
                    .createdAt(audit.getCreatedAt())
                    .build();
        }).toList();
    }
}
