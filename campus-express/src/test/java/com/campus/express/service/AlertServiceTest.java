package com.campus.express.service;

import com.campus.express.entity.AlertRecord;
import com.campus.express.entity.ExpressPackage;
import com.campus.express.mapper.AlertRecordMapper;
import com.campus.express.mapper.ExpressPackageMapper;
import com.campus.express.mapper.StationMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private AlertRecordMapper alertRecordMapper;
    @Mock
    private ExpressPackageMapper packageMapper;
    @Mock
    private StationMapper stationMapper;

    @Test
    void shouldNotDuplicateExistingTimeoutAlert() {
        AlertService service = new AlertService(alertRecordMapper, packageMapper, stationMapper);
        when(packageMapper.selectList(any())).thenReturn(List.of(
                overduePackage(1L, 101L),
                overduePackage(2L, 102L)
        ));
        when(alertRecordMapper.selectCount(any())).thenReturn(0L, 1L);

        int created = service.scanOverdueAlerts();

        assertEquals(1, created);
        ArgumentCaptor<AlertRecord> captor = ArgumentCaptor.forClass(AlertRecord.class);
        verify(alertRecordMapper, times(1)).insert(captor.capture());
        assertEquals(101L, captor.getValue().getPackageId());
    }

    private ExpressPackage overduePackage(Long stationId, Long packageId) {
        ExpressPackage expressPackage = new ExpressPackage();
        expressPackage.setId(packageId);
        expressPackage.setStationId(stationId);
        expressPackage.setPickupCode("D3000001");
        expressPackage.setStatus(0);
        expressPackage.setInboundAt(LocalDateTime.now().minusHours(49));
        return expressPackage;
    }
}
