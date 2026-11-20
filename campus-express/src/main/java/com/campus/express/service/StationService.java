package com.campus.express.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.express.common.PageResult;
import com.campus.express.common.enums.Role;
import com.campus.express.common.exception.BusinessException;
import com.campus.express.dto.station.StationCompanyRequest;
import com.campus.express.dto.station.StationSaveRequest;
import com.campus.express.entity.Station;
import com.campus.express.entity.StationCompany;
import com.campus.express.mapper.StationCompanyMapper;
import com.campus.express.mapper.StationMapper;
import com.campus.express.security.AuthUser;
import com.campus.express.security.UserContext;
import com.campus.express.vo.station.CompanyVO;
import com.campus.express.vo.station.StationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 站点与品牌业务
 */
@Service
@RequiredArgsConstructor
public class StationService {

    private final StationMapper stationMapper;
    private final StationCompanyMapper stationCompanyMapper;
    private final AlertService alertService;

    public PageResult<StationVO> list(int page, int pageSize) {
        Page<Station> result = stationMapper.selectPage(
                new Page<>(page, pageSize),
                Wrappers.<Station>lambdaQuery()
                        .eq(Station::getStatus, 1)
                        .orderByAsc(Station::getId)
        );
        return new PageResult<>(result.getTotal(), toStationVOs(result.getRecords()));
    }

    public StationVO get(Long id) {
        Station station = stationMapper.selectById(id);
        if (station == null || station.getStatus() != 1) {
            throw BusinessException.notFound("站点不存在或已停用");
        }
        return toStationVOs(List.of(station)).getFirst();
    }

    public List<Station> listMapStations() {
        return stationMapper.selectList(Wrappers.<Station>lambdaQuery()
                .eq(Station::getStatus, 1)
                .isNotNull(Station::getLng)
                .isNotNull(Station::getLat)
                .orderByAsc(Station::getId));
    }

    public List<Station> nearby(double lng, double lat) {
        return listMapStations().stream()
                .sorted(Comparator.comparingDouble(station -> distance(
                        lng,
                        lat,
                        station.getLng().doubleValue(),
                        station.getLat().doubleValue()
                )))
                .toList();
    }

    public List<CompanyVO> listCompanies() {
        Map<String, String> companies = new LinkedHashMap<>();
        stationCompanyMapper.selectList(Wrappers.<StationCompany>lambdaQuery()
                        .orderByAsc(StationCompany::getCompanyCode))
                .forEach(company -> companies.putIfAbsent(company.getCompanyCode(), company.getCompanyName()));
        return companies.entrySet().stream()
                .map(entry -> new CompanyVO(entry.getKey(), entry.getValue()))
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public void create(StationSaveRequest request) {
        Station station = new Station();
        copyEditableFields(request, station, true);
        try {
            stationMapper.insert(station);
        } catch (DataIntegrityViolationException exception) {
            throw BusinessException.badRequest("站点名称已存在");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, StationSaveRequest request) {
        Station existing = requireStation(id);
        AuthUser user = UserContext.require();
        if (user.role() == Role.STATION_ADMIN.getCode()) {
            if (!id.equals(user.stationId())) {
                throw BusinessException.forbidden("只能修改本站点信息");
            }
            copyEditableFields(request, existing, false);
        } else {
            copyEditableFields(request, existing, true);
        }

        try {
            stationMapper.updateById(existing);
        } catch (DataIntegrityViolationException exception) {
            throw BusinessException.badRequest("站点名称已存在");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void disable(Long id) {
        Station station = requireStation(id);
        if (station.getStatus() == 0) {
            return;
        }
        station.setStatus(0);
        stationMapper.updateById(station);
        alertService.createStationDisabledAlert(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateCompanies(Long stationId, List<StationCompanyRequest> requests) {
        requireStation(stationId);
        Map<String, String> uniqueCompanies = new LinkedHashMap<>();
        for (StationCompanyRequest request : requests) {
            String code = request.getCompanyCode().trim().toUpperCase();
            if (uniqueCompanies.putIfAbsent(code, request.getCompanyName().trim()) != null) {
                throw BusinessException.badRequest("快递公司编码不能重复：" + code);
            }
        }

        Map<String, StationCompany> existing = stationCompanyMapper.selectList(
                        Wrappers.<StationCompany>lambdaQuery()
                                .eq(StationCompany::getStationId, stationId))
                .stream()
                .collect(Collectors.toMap(StationCompany::getCompanyCode, company -> company));

        for (StationCompany company : existing.values()) {
            if (!uniqueCompanies.containsKey(company.getCompanyCode())) {
                try {
                    stationCompanyMapper.deleteById(company.getId());
                } catch (DataIntegrityViolationException exception) {
                    throw BusinessException.badRequest(
                            "快递公司已有包裹数据，不能取消代理：" + company.getCompanyCode());
                }
            }
        }

        for (Map.Entry<String, String> entry : uniqueCompanies.entrySet()) {
            StationCompany company = existing.get(entry.getKey());
            if (company == null) {
                company = new StationCompany();
                company.setStationId(stationId);
                company.setCompanyCode(entry.getKey());
            }
            company.setCompanyName(entry.getValue());
            if (company.getId() == null) {
                stationCompanyMapper.insert(company);
            } else {
                stationCompanyMapper.updateById(company);
            }
        }
    }

    private Station requireStation(Long id) {
        Station station = stationMapper.selectById(id);
        if (station == null) {
            throw BusinessException.notFound("站点不存在");
        }
        return station;
    }

    private List<StationVO> toStationVOs(List<Station> stations) {
        if (stations.isEmpty()) {
            return List.of();
        }
        Set<Long> stationIds = stations.stream().map(Station::getId).collect(Collectors.toSet());
        Map<Long, List<CompanyVO>> companyMap = stationCompanyMapper.selectList(
                        Wrappers.<StationCompany>lambdaQuery()
                                .in(StationCompany::getStationId, stationIds)
                                .orderByAsc(StationCompany::getCompanyCode))
                .stream()
                .collect(Collectors.groupingBy(
                        StationCompany::getStationId,
                        Collectors.mapping(
                                company -> new CompanyVO(company.getCompanyCode(), company.getCompanyName()),
                                Collectors.toList()
                        )
                ));

        return stations.stream()
                .map(station -> StationVO.builder()
                        .id(station.getId())
                        .name(station.getName())
                        .address(station.getAddress())
                        .lng(station.getLng())
                        .lat(station.getLat())
                        .businessHours(station.getBusinessHours())
                        .contactPhone(station.getContactPhone())
                        .status(station.getStatus())
                        .crowdLevel(station.getCrowdLevel())
                        .crowdScore(station.getCrowdScore())
                        .crowdUpdatedAt(station.getCrowdUpdatedAt())
                        .companies(companyMap.getOrDefault(station.getId(), List.of()))
                        .build())
                .toList();
    }

    private void copyEditableFields(StationSaveRequest request, Station station, boolean allowManagementFields) {
        station.setAddress(request.getAddress().trim());
        if (request.getLng() != null) {
            station.setLng(request.getLng());
        }
        if (request.getLat() != null) {
            station.setLat(request.getLat());
        }
        station.setBusinessHours(request.getBusinessHours().trim());
        station.setContactPhone(StringUtils.hasText(request.getContactPhone())
                ? request.getContactPhone().trim()
                : null);
        if (allowManagementFields) {
            station.setName(request.getName().trim());
            if (station.getId() == null) {
                station.setStatus(request.getStatus() == null ? 1 : request.getStatus());
            } else if (request.getStatus() != null) {
                station.setStatus(request.getStatus());
            }
        }
    }

    private double distance(double lng1, double lat1, double lng2, double lat2) {
        double earthRadius = 6371000;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lngDistance = Math.toRadians(lng2 - lng1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);
        return earthRadius * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
