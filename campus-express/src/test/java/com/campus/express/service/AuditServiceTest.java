package com.campus.express.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.campus.express.dto.audit.AuditReviewRequest;
import com.campus.express.entity.StationAudit;
import com.campus.express.entity.SysUser;
import com.campus.express.mapper.StationAuditMapper;
import com.campus.express.mapper.StationMapper;
import com.campus.express.mapper.SysUserMapper;
import com.campus.express.security.AuthUser;
import com.campus.express.security.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.apache.ibatis.builder.MapperBuilderAssistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "test"),
                StationAudit.class
        );
    }

    @Mock
    private StationAuditMapper auditMapper;
    @Mock
    private SysUserMapper userMapper;
    @Mock
    private StationMapper stationMapper;

    @AfterEach
    void clearContext() {
        UserContext.clear();
    }

    @Test
    void shouldPromoteApplicantAfterApproval() {
        AuditService service = new AuditService(auditMapper, userMapper, stationMapper);
        StationAudit audit = new StationAudit();
        audit.setId(1L);
        audit.setUserId(7L);
        audit.setStationId(3L);
        audit.setStatus(0);
        SysUser user = new SysUser();
        user.setId(7L);
        user.setStatus(1);
        user.setRole(0);

        when(auditMapper.selectById(1L)).thenReturn(audit);
        when(auditMapper.update(any(), any())).thenReturn(1);
        when(userMapper.selectById(7L)).thenReturn(user);
        UserContext.set(new AuthUser(99L, 2, null));
        AuditReviewRequest request = new AuditReviewRequest();
        request.setStatus(1);

        service.review(1L, request);

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(userMapper).updateById(captor.capture());
        assertEquals(1, captor.getValue().getRole());
        assertEquals(3L, captor.getValue().getStationId());
        assertEquals(1, captor.getValue().getStatus());
    }
}
