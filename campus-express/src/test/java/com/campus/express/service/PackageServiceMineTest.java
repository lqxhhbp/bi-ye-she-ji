package com.campus.express.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.express.common.PageResult;
import com.campus.express.common.exception.BusinessException;
import com.campus.express.entity.ExpressPackage;
import com.campus.express.entity.Station;
import com.campus.express.entity.SysUser;
import com.campus.express.mapper.ExpressPackageMapper;
import com.campus.express.mapper.PackageLogMapper;
import com.campus.express.mapper.StationCompanyMapper;
import com.campus.express.mapper.StationMapper;
import com.campus.express.mapper.SysUserMapper;
import com.campus.express.security.AuthUser;
import com.campus.express.security.UserContext;
import com.campus.express.vo.pkg.PackageVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PackageServiceMineTest {

    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "test"),
                ExpressPackage.class
        );
    }

    @Mock
    private ExpressPackageMapper packageMapper;
    @Mock
    private PackageLogMapper packageLogMapper;
    @Mock
    private StationMapper stationMapper;
    @Mock
    private StationCompanyMapper stationCompanyMapper;
    @Mock
    private SysUserMapper userMapper;
    @Mock
    private PackageQueryRateLimiter rateLimiter;
    @Mock
    private AlertService alertService;
    @Mock
    private IdleService idleService;

    @AfterEach
    void clearContext() {
        UserContext.clear();
    }

    @Test
    void shouldQueryPackagesByCurrentUserPhone() {
        UserContext.set(new AuthUser(8L, 0, null));
        when(userMapper.selectById(8L)).thenReturn(student(8L, "13800002001"));

        ExpressPackage expressPackage = expressPackage(11L, "13800002001", 1);
        Page<ExpressPackage> page = new Page<>(1, 20);
        page.setTotal(1);
        page.setRecords(List.of(expressPackage));
        when(packageMapper.selectPage(any(), any())).thenReturn(page);

        Station station = new Station();
        station.setId(3L);
        station.setName("测试站点");
        when(stationMapper.selectBatchIds(any())).thenReturn(List.of(station));
        when(stationCompanyMapper.selectList(any())).thenReturn(List.of());

        PageResult<PackageVO> result = service().mine(1, 20, 1);

        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals("138****2001", result.getRecords().getFirst().getReceiverPhone());
        assertEquals("测试站点", result.getRecords().getFirst().getStationName());
    }

    @Test
    void shouldReturnEmptyPageWhenCurrentUserHasNoPackages() {
        UserContext.set(new AuthUser(9L, 0, null));
        when(userMapper.selectById(9L)).thenReturn(student(9L, "13800002002"));
        Page<ExpressPackage> page = new Page<>(1, 20);
        page.setTotal(0);
        page.setRecords(List.of());
        when(packageMapper.selectPage(any(), any())).thenReturn(page);

        PageResult<PackageVO> result = service().mine(1, 20, null);

        assertEquals(0, result.getTotal());
        assertTrue(result.getRecords().isEmpty());
    }

    @Test
    void shouldRejectInvalidStatus() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service().mine(1, 20, 3)
        );

        assertEquals(400, exception.getCode());
        assertEquals("包裹状态必须为 0、1 或 2", exception.getMessage());
    }

    private PackageService service() {
        return new PackageService(
                packageMapper,
                packageLogMapper,
                stationMapper,
                stationCompanyMapper,
                userMapper,
                rateLimiter,
                alertService,
                idleService
        );
    }

    private SysUser student(Long id, String phone) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setPhone(phone);
        return user;
    }

    private ExpressPackage expressPackage(Long id, String phone, int status) {
        ExpressPackage expressPackage = new ExpressPackage();
        expressPackage.setId(id);
        expressPackage.setPickupCode("P4TEST1");
        expressPackage.setReceiverPhone(phone);
        expressPackage.setStationId(3L);
        expressPackage.setCompanyCode("SF");
        expressPackage.setShelfNo("A-01");
        expressPackage.setStatus(status);
        expressPackage.setInboundAt(LocalDateTime.now());
        return expressPackage;
    }
}
