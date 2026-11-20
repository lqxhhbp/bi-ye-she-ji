package com.campus.express.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.express.common.PageResult;
import com.campus.express.common.exception.BusinessException;
import com.campus.express.common.util.PhoneMaskUtil;
import com.campus.express.dto.pkg.PackageCreateRequest;
import com.campus.express.dto.pkg.PackageImportRow;
import com.campus.express.dto.pkg.VerifyPackageRequest;
import com.campus.express.entity.ExpressPackage;
import com.campus.express.entity.PackageLog;
import com.campus.express.entity.Station;
import com.campus.express.entity.StationCompany;
import com.campus.express.entity.SysUser;
import com.campus.express.mapper.ExpressPackageMapper;
import com.campus.express.mapper.PackageLogMapper;
import com.campus.express.mapper.StationCompanyMapper;
import com.campus.express.mapper.StationMapper;
import com.campus.express.mapper.SysUserMapper;
import com.campus.express.security.AuthUser;
import com.campus.express.security.UserContext;
import com.campus.express.vo.pkg.ImportErrorVO;
import com.campus.express.vo.pkg.ImportResultVO;
import com.campus.express.vo.pkg.PackageExcelRow;
import com.campus.express.vo.pkg.PackageTimelineVO;
import com.campus.express.vo.pkg.PackageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 包裹上架、导入、查件与撤销业务
 */
@Service
@RequiredArgsConstructor
public class PackageService {

    private static final Pattern PICKUP_CODE_PATTERN = Pattern.compile("^[0-9A-Za-z]{6,8}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1\\d{10}$");

    private final ExpressPackageMapper packageMapper;
    private final PackageLogMapper packageLogMapper;
    private final StationMapper stationMapper;
    private final StationCompanyMapper stationCompanyMapper;
    private final SysUserMapper userMapper;
    private final PackageQueryRateLimiter rateLimiter;
    private final AlertService alertService;
    private final IdleService idleService;

    @Transactional(rollbackFor = Exception.class)
    public PackageVO create(PackageCreateRequest request) {
        Long stationId = currentStationId();
        String pickupCode = request.getPickupCode().trim().toUpperCase();
        String companyCode = request.getCompanyCode().trim().toUpperCase();
        ensureSupportedCompany(stationId, companyCode);

        Long existing = packageMapper.selectCount(Wrappers.<ExpressPackage>lambdaQuery()
                .eq(ExpressPackage::getPickupCode, pickupCode));
        if (existing > 0) {
            throw BusinessException.badRequest("取件码已存在");
        }

        ExpressPackage expressPackage = new ExpressPackage();
        expressPackage.setPickupCode(pickupCode);
        expressPackage.setReceiverPhone(request.getReceiverPhone().trim());
        expressPackage.setStationId(stationId);
        expressPackage.setCompanyCode(companyCode);
        expressPackage.setShelfNo(request.getShelfNo().trim());
        expressPackage.setStatus(0);
        expressPackage.setOperatorId(UserContext.requireUserId());
        expressPackage.setInboundAt(LocalDateTime.now());
        expressPackage.setRemark(StringUtils.hasText(request.getRemark()) ? request.getRemark().trim() : null);
        try {
            packageMapper.insert(expressPackage);
        } catch (DuplicateKeyException exception) {
            throw BusinessException.badRequest("取件码已存在");
        }
        insertLog(expressPackage.getId(), 0, expressPackage.getOperatorId());
        return toPackageVOs(List.of(expressPackage)).getFirst();
    }

    public PageResult<PackageVO> list(int page, int pageSize, Integer status, String keyword) {
        Long stationId = currentStationId();
        var query = Wrappers.<ExpressPackage>lambdaQuery()
                .eq(ExpressPackage::getStationId, stationId)
                .eq(status != null, ExpressPackage::getStatus, status)
                .orderByDesc(ExpressPackage::getInboundAt);
        if (StringUtils.hasText(keyword)) {
            String value = keyword.trim();
            query.and(wrapper -> wrapper
                    .like(ExpressPackage::getPickupCode, value)
                    .or()
                    .like(ExpressPackage::getReceiverPhone, value)
                    .or()
                    .like(ExpressPackage::getShelfNo, value));
        }

        Page<ExpressPackage> result = packageMapper.selectPage(
                new Page<>(Math.max(page, 1), Math.min(Math.max(pageSize, 1), 100)),
                query
        );
        return new PageResult<>(result.getTotal(), toPackageVOs(result.getRecords()));
    }

    public PackageVO get(Long id) {
        return toPackageVOs(List.of(requireOwnedPackage(id))).getFirst();
    }

    @Transactional(rollbackFor = Exception.class)
    public void revoke(Long id) {
        Long stationId = currentStationId();
        ExpressPackage expressPackage = requireOwnedPackage(id);
        if (expressPackage.getStatus() != 0) {
            throw BusinessException.badRequest("只有待取包裹可以撤销");
        }
        if (expressPackage.getInboundAt().isBefore(LocalDateTime.now().minusMinutes(30))) {
            throw BusinessException.badRequest("包裹上架已超过 30 分钟，不能撤销");
        }

        int updated = packageMapper.update(
                null,
                Wrappers.<ExpressPackage>lambdaUpdate()
                        .eq(ExpressPackage::getId, id)
                        .eq(ExpressPackage::getStationId, stationId)
                        .eq(ExpressPackage::getStatus, 0)
                        .ge(ExpressPackage::getInboundAt, LocalDateTime.now().minusMinutes(30))
                        .set(ExpressPackage::getStatus, 2)
        );
        if (updated == 0) {
            throw BusinessException.badRequest("包裹状态已变化，撤销失败");
        }
        insertLog(id, 1, UserContext.requireUserId());
    }

    public Map<String, Long> todayStats() {
        Long stationId = currentStationId();
        LocalDateTime start = LocalDate.now().atStartOfDay();
        Map<String, Long> result = new LinkedHashMap<>();
        result.put("inbound", packageMapper.selectCount(Wrappers.<ExpressPackage>lambdaQuery()
                .eq(ExpressPackage::getStationId, stationId)
                .ge(ExpressPackage::getInboundAt, start)));
        result.put("pending", packageMapper.selectCount(Wrappers.<ExpressPackage>lambdaQuery()
                .eq(ExpressPackage::getStationId, stationId)
                .eq(ExpressPackage::getStatus, 0)));
        result.put("taken", packageMapper.selectCount(Wrappers.<ExpressPackage>lambdaQuery()
                .eq(ExpressPackage::getStationId, stationId)
                .eq(ExpressPackage::getStatus, 1)
                .ge(ExpressPackage::getTakenAt, start)));
        result.put("revoked", packageMapper.selectCount(Wrappers.<ExpressPackage>lambdaQuery()
                .eq(ExpressPackage::getStationId, stationId)
                .eq(ExpressPackage::getStatus, 2)
                .ge(ExpressPackage::getInboundAt, start)));
        return result;
    }

    public byte[] exportExcel() {
        Long stationId = currentStationId();
        List<ExpressPackage> packages = packageMapper.selectList(Wrappers.<ExpressPackage>lambdaQuery()
                .eq(ExpressPackage::getStationId, stationId)
                .orderByDesc(ExpressPackage::getInboundAt));
        Map<Long, String> stationNames = packages.isEmpty()
                ? Map.of()
                : stationMapper.selectBatchIds(
                                packages.stream().map(ExpressPackage::getStationId).collect(Collectors.toSet()))
                        .stream()
                        .collect(Collectors.toMap(Station::getId, Station::getName));
        Map<String, String> companyNames = companyNameMap(packages);

        List<PackageExcelRow> rows = packages.stream().map(item -> {
            PackageExcelRow row = new PackageExcelRow();
            row.setPickupCode(item.getPickupCode());
            row.setReceiverPhone(PhoneMaskUtil.mask(item.getReceiverPhone()));
            row.setStationName(stationNames.get(item.getStationId()));
            row.setCompanyName(companyNames.getOrDefault(companyKey(item.getStationId(), item.getCompanyCode()),
                    item.getCompanyCode()));
            row.setShelfNo(item.getShelfNo());
            row.setStatusName(statusName(item.getStatus()));
            row.setInboundAt(item.getInboundAt());
            row.setTakenAt(item.getTakenAt());
            return row;
        }).toList();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        EasyExcel.write(output, PackageExcelRow.class).sheet("包裹").doWrite(rows);
        return output.toByteArray();
    }

    public byte[] importTemplate() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        EasyExcel.write(output, PackageImportRow.class).sheet("包裹导入").doWrite(List.of());
        return output.toByteArray();
    }

    @Transactional(rollbackFor = Exception.class)
    public ImportResultVO importExcel(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.badRequest("请选择 Excel 文件");
        }

        ImportListener listener = new ImportListener();
        try {
            EasyExcel.read(file.getInputStream(), PackageImportRow.class, listener)
                    .sheet()
                    .doRead();
        } catch (IOException | RuntimeException exception) {
            throw BusinessException.badRequest("Excel 文件读取失败");
        }

        Long stationId = currentStationId();
        Map<String, String> supportedCompanies = stationCompanyMapper.selectList(
                        Wrappers.<StationCompany>lambdaQuery()
                                .eq(StationCompany::getStationId, stationId))
                .stream()
                .collect(Collectors.toMap(StationCompany::getCompanyCode, StationCompany::getCompanyName));
        if (supportedCompanies.isEmpty()) {
            throw BusinessException.badRequest("本站点尚未配置代理快递公司");
        }

        List<ImportErrorVO> errors = new ArrayList<>();
        Set<String> seenCodes = new HashSet<>();
        List<ImportItem> validItems = new ArrayList<>();
        for (ImportItem item : listener.items) {
            PackageImportRow row = item.row;
            String pickupCode = trim(row.getPickupCode()).toUpperCase();
            String phone = trim(row.getReceiverPhone());
            String companyCode = trim(row.getCompanyCode()).toUpperCase();
            String shelfNo = trim(row.getShelfNo());

            if (!PICKUP_CODE_PATTERN.matcher(pickupCode).matches()) {
                errors.add(new ImportErrorVO(item.rowNumber, "取件码必须为 6-8 位数字或字母"));
            } else if (!seenCodes.add(pickupCode)) {
                errors.add(new ImportErrorVO(item.rowNumber, "文件内取件码重复：" + pickupCode));
            }
            if (!PHONE_PATTERN.matcher(phone).matches()) {
                errors.add(new ImportErrorVO(item.rowNumber, "收件人手机号格式不正确"));
            }
            if (!supportedCompanies.containsKey(companyCode)) {
                errors.add(new ImportErrorVO(item.rowNumber, "本站点不代理快递公司：" + companyCode));
            }
            if (!StringUtils.hasText(shelfNo)) {
                errors.add(new ImportErrorVO(item.rowNumber, "货架号不能为空"));
            }

            row.setPickupCode(pickupCode);
            row.setReceiverPhone(phone);
            row.setCompanyCode(companyCode);
            row.setShelfNo(shelfNo);
            validItems.add(item);
        }

        Set<String> codes = validItems.stream()
                .map(item -> item.row.getPickupCode())
                .filter(PICKUP_CODE_PATTERN.asMatchPredicate())
                .collect(Collectors.toSet());
        if (!codes.isEmpty()) {
            Set<String> existingCodes = packageMapper.selectList(Wrappers.<ExpressPackage>lambdaQuery()
                            .in(ExpressPackage::getPickupCode, codes))
                    .stream()
                    .map(ExpressPackage::getPickupCode)
                    .collect(Collectors.toSet());
            for (ImportItem item : validItems) {
                if (existingCodes.contains(item.row.getPickupCode())) {
                    errors.add(new ImportErrorVO(
                            item.rowNumber,
                            "取件码已存在：" + item.row.getPickupCode()
                    ));
                }
            }
        }

        if (!errors.isEmpty()) {
            return ImportResultVO.builder()
                    .total(listener.items.size())
                    .success(0)
                    .failed((int) errors.stream().map(ImportErrorVO::getRowNumber).distinct().count())
                    .errors(errors)
                    .build();
        }

        Long operatorId = UserContext.requireUserId();
        LocalDateTime inboundAt = LocalDateTime.now();
        for (ImportItem item : validItems) {
            ExpressPackage expressPackage = new ExpressPackage();
            expressPackage.setPickupCode(item.row.getPickupCode());
            expressPackage.setReceiverPhone(item.row.getReceiverPhone());
            expressPackage.setStationId(stationId);
            expressPackage.setCompanyCode(item.row.getCompanyCode());
            expressPackage.setShelfNo(item.row.getShelfNo());
            expressPackage.setStatus(0);
            expressPackage.setOperatorId(operatorId);
            expressPackage.setInboundAt(inboundAt);
            expressPackage.setRemark(StringUtils.hasText(item.row.getRemark()) ? item.row.getRemark().trim() : null);
            packageMapper.insert(expressPackage);
            insertLog(expressPackage.getId(), 0, operatorId);
        }

        return ImportResultVO.builder()
                .total(validItems.size())
                .success(validItems.size())
                .failed(0)
                .errors(List.of())
                .build();
    }

    public List<PackageVO> queryByPhone(String phone, String ip) {
        rateLimiter.check("phone", ip + ":" + phone);
        List<ExpressPackage> packages = packageMapper.selectList(Wrappers.<ExpressPackage>lambdaQuery()
                .eq(ExpressPackage::getReceiverPhone, phone.trim())
                .eq(ExpressPackage::getStatus, 0)
                .orderByDesc(ExpressPackage::getInboundAt));
        return toPackageVOs(packages);
    }

    public PackageVO queryByPickupCode(String pickupCode, String ip) {
        rateLimiter.check("pickup", ip + ":" + pickupCode);
        ExpressPackage expressPackage = packageMapper.selectOne(Wrappers.<ExpressPackage>lambdaQuery()
                .eq(ExpressPackage::getPickupCode, pickupCode.trim().toUpperCase())
                .eq(ExpressPackage::getStatus, 0));
        if (expressPackage == null) {
            throw BusinessException.notFound("未找到待取包裹");
        }
        return toPackageVOs(List.of(expressPackage)).getFirst();
    }

    public PageResult<PackageVO> mine(int page, int pageSize, Integer status) {
        if (status != null && (status < 0 || status > 2)) {
            throw BusinessException.badRequest("包裹状态必须为 0、1 或 2");
        }

        SysUser user = userMapper.selectById(UserContext.requireUserId());
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }

        Page<ExpressPackage> result = packageMapper.selectPage(
                new Page<>(Math.max(page, 1), Math.min(Math.max(pageSize, 1), 100)),
                Wrappers.<ExpressPackage>lambdaQuery()
                        .eq(ExpressPackage::getReceiverPhone, user.getPhone())
                        .eq(status != null, ExpressPackage::getStatus, status)
                        .orderByDesc(ExpressPackage::getInboundAt)
        );
        return new PageResult<>(result.getTotal(), toPackageVOs(result.getRecords()));
    }

    @Transactional(rollbackFor = Exception.class)
    public PackageVO verify(VerifyPackageRequest request, String ip) {
        Long stationId = currentStationId();
        ExpressPackage expressPackage = packageMapper.selectOne(Wrappers.<ExpressPackage>lambdaQuery()
                .eq(ExpressPackage::getPickupCode, request.getPickupCode().trim().toUpperCase())
                .eq(ExpressPackage::getStationId, stationId)
                .eq(ExpressPackage::getStatus, 0));
        if (expressPackage == null) {
            throw BusinessException.notFound("本站点未找到待取包裹");
        }

        LocalDateTime takenAt = LocalDateTime.now();
        int updated = packageMapper.update(
                null,
                Wrappers.<ExpressPackage>lambdaUpdate()
                        .eq(ExpressPackage::getId, expressPackage.getId())
                        .eq(ExpressPackage::getStationId, stationId)
                        .eq(ExpressPackage::getStatus, 0)
                        .set(ExpressPackage::getStatus, 1)
                        .set(ExpressPackage::getTakenAt, takenAt)
        );
        if (updated == 0) {
            throw BusinessException.badRequest("包裹已被核销或状态已变化");
        }

        Long operatorId = UserContext.requireUserId();
        insertLog(expressPackage.getId(), 2, operatorId);
        alertService.closeTimeoutAlert(expressPackage.getId());
        idleService.addCalibrationIfAllowed(stationId, 0, ip, operatorId);

        expressPackage.setStatus(1);
        expressPackage.setTakenAt(takenAt);
        return toPackageVOs(List.of(expressPackage)).getFirst();
    }

    public PackageVO getVerifiablePackage(String pickupCode) {
        Long stationId = currentStationId();
        ExpressPackage expressPackage = packageMapper.selectOne(Wrappers.<ExpressPackage>lambdaQuery()
                .eq(ExpressPackage::getPickupCode, pickupCode.trim().toUpperCase())
                .eq(ExpressPackage::getStationId, stationId)
                .eq(ExpressPackage::getStatus, 0));
        if (expressPackage == null) {
            throw BusinessException.notFound("本站点未找到待取包裹");
        }
        return toPackageVOs(List.of(expressPackage)).getFirst();
    }

    public PageResult<PackageVO> listTaken(int page, int pageSize) {
        Page<ExpressPackage> result = packageMapper.selectPage(
                new Page<>(Math.max(page, 1), Math.min(Math.max(pageSize, 1), 100)),
                Wrappers.<ExpressPackage>lambdaQuery()
                        .eq(ExpressPackage::getStationId, currentStationId())
                        .eq(ExpressPackage::getStatus, 1)
                        .orderByDesc(ExpressPackage::getTakenAt)
        );
        return new PageResult<>(result.getTotal(), toPackageVOs(result.getRecords()));
    }

    public PageResult<PackageVO> listOverdue(int page, int pageSize) {
        Page<ExpressPackage> result = packageMapper.selectPage(
                new Page<>(Math.max(page, 1), Math.min(Math.max(pageSize, 1), 100)),
                Wrappers.<ExpressPackage>lambdaQuery()
                        .eq(ExpressPackage::getStationId, currentStationId())
                        .eq(ExpressPackage::getStatus, 0)
                        .lt(ExpressPackage::getInboundAt, LocalDateTime.now().minusHours(48))
                        .orderByAsc(ExpressPackage::getInboundAt)
        );
        return new PageResult<>(result.getTotal(), toPackageVOs(result.getRecords()));
    }

    public List<PackageTimelineVO> timeline(Long packageId) {
        SysUser user = userMapper.selectById(UserContext.requireUserId());
        ExpressPackage expressPackage = packageMapper.selectById(packageId);
        if (user == null || expressPackage == null) {
            throw BusinessException.notFound("包裹不存在");
        }
        if (!user.getPhone().equals(expressPackage.getReceiverPhone())) {
            throw BusinessException.forbidden("只能查看本人包裹的时间线");
        }
        return packageLogMapper.selectList(Wrappers.<PackageLog>lambdaQuery()
                        .eq(PackageLog::getPackageId, packageId)
                        .orderByAsc(PackageLog::getCreatedAt))
                .stream()
                .map(log -> new PackageTimelineVO(log.getAction(), actionName(log.getAction()), log.getCreatedAt()))
                .toList();
    }

    private ExpressPackage requireOwnedPackage(Long id) {
        ExpressPackage expressPackage = packageMapper.selectById(id);
        if (expressPackage == null || !currentStationId().equals(expressPackage.getStationId())) {
            throw BusinessException.notFound("包裹不存在");
        }
        return expressPackage;
    }

    private void ensureSupportedCompany(Long stationId, String companyCode) {
        Long count = stationCompanyMapper.selectCount(Wrappers.<StationCompany>lambdaQuery()
                .eq(StationCompany::getStationId, stationId)
                .eq(StationCompany::getCompanyCode, companyCode));
        if (count == 0) {
            throw BusinessException.badRequest("本站点不代理快递公司：" + companyCode);
        }
    }

    private Long currentStationId() {
        AuthUser user = UserContext.require();
        if (user.stationId() == null) {
            throw BusinessException.forbidden("当前账号未绑定站点");
        }
        Station station = stationMapper.selectById(user.stationId());
        if (station == null || station.getStatus() != 1) {
            throw BusinessException.forbidden("当前站点不存在或已停用");
        }
        return user.stationId();
    }

    private void insertLog(Long packageId, int action, Long operatorId) {
        PackageLog log = new PackageLog();
        log.setPackageId(packageId);
        log.setAction(action);
        log.setOperatorId(operatorId);
        packageLogMapper.insert(log);
    }

    private List<PackageVO> toPackageVOs(List<ExpressPackage> packages) {
        if (packages.isEmpty()) {
            return List.of();
        }
        Map<Long, Station> stations = stationMapper.selectBatchIds(
                        packages.stream().map(ExpressPackage::getStationId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Station::getId, Function.identity()));
        Map<String, String> companyNames = companyNameMap(packages);

        return packages.stream().map(item -> {
            Station station = stations.get(item.getStationId());
            return PackageVO.builder()
                    .id(item.getId())
                    .pickupCode(item.getPickupCode())
                    .receiverPhone(PhoneMaskUtil.mask(item.getReceiverPhone()))
                    .stationId(item.getStationId())
                    .stationName(station == null ? null : station.getName())
                    .companyCode(item.getCompanyCode())
                    .companyName(companyNames.getOrDefault(
                            companyKey(item.getStationId(), item.getCompanyCode()),
                            item.getCompanyCode()
                    ))
                    .shelfNo(item.getShelfNo())
                    .status(item.getStatus())
                    .inboundAt(item.getInboundAt())
                    .takenAt(item.getTakenAt())
                    .remark(item.getRemark())
                    .build();
        }).toList();
    }

    private Map<String, String> companyNameMap(Collection<ExpressPackage> packages) {
        if (packages.isEmpty()) {
            return Map.of();
        }
        Set<Long> stationIds = packages.stream().map(ExpressPackage::getStationId).collect(Collectors.toSet());
        return stationCompanyMapper.selectList(Wrappers.<StationCompany>lambdaQuery()
                        .in(StationCompany::getStationId, stationIds))
                .stream()
                .collect(Collectors.toMap(
                        company -> companyKey(company.getStationId(), company.getCompanyCode()),
                        StationCompany::getCompanyName
                ));
    }

    private String companyKey(Long stationId, String companyCode) {
        return stationId + ":" + companyCode;
    }

    private String statusName(Integer status) {
        return switch (status) {
            case 0 -> "待取";
            case 1 -> "已取";
            case 2 -> "已撤销";
            default -> "未知";
        };
    }

    private String actionName(Integer action) {
        return switch (action) {
            case 0 -> "上架";
            case 1 -> "撤销";
            case 2 -> "核销";
            default -> "未知";
        };
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static final class ImportListener extends AnalysisEventListener<PackageImportRow> {

        private final List<ImportItem> items = new ArrayList<>();

        @Override
        public void invoke(PackageImportRow row, AnalysisContext context) {
            items.add(new ImportItem(context.readRowHolder().getRowIndex() + 1, row));
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {
        }
    }

    private record ImportItem(int rowNumber, PackageImportRow row) {
    }
}
