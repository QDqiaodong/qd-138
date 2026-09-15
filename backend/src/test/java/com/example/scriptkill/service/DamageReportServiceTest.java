package com.example.scriptkill.service;

import com.example.scriptkill.dto.request.DamageReportCreateRequest;
import com.example.scriptkill.dto.response.DamageReportResponse;
import com.example.scriptkill.entity.Prop;
import com.example.scriptkill.entity.PropDamageReport;
import com.example.scriptkill.exception.BusinessException;
import com.example.scriptkill.repository.PropDamageReportRepository;
import com.example.scriptkill.repository.PropRepository;
import com.example.scriptkill.service.impl.DamageReportServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DamageReportServiceTest {

    @Mock
    private PropDamageReportRepository damageReportRepository;

    @Mock
    private PropRepository propRepository;

    @InjectMocks
    private DamageReportServiceImpl damageReportService;

    private Prop prop;

    @BeforeEach
    void setUp() {
        prop = new Prop();
        prop.setId(5L);
        prop.setPropCode("PROP-005");
        prop.setPropName("长剑");
        prop.setStatus("正常");
    }

    private DamageReportCreateRequest request(String damagedPart, String discoverer) {
        DamageReportCreateRequest req = new DamageReportCreateRequest();
        req.setPropId(5L);
        req.setDamagedPart(damagedPart);
        req.setDiscoverer(discoverer);
        return req;
    }

    private PropDamageReport openReport() {
        PropDamageReport report = new PropDamageReport();
        report.setId(1L);
        report.setReportNo("BS000001");
        report.setPropId(5L);
        report.setDamagedPart("剑柄缠绳断裂处");
        report.setDiscoverer("场务小李");
        report.setStatus(PropDamageReport.STATUS_OPEN);
        return report;
    }

    @Test
    void 开单成功_记录损坏部位和发现人_道具状态置为损坏() {
        when(propRepository.findById(5L)).thenReturn(Optional.of(prop));
        when(damageReportRepository.findFirstByPropIdAndStatus(5L, PropDamageReport.STATUS_OPEN))
                .thenReturn(Optional.empty());
        when(damageReportRepository.saveAndFlush(any(PropDamageReport.class))).thenAnswer(inv -> {
            PropDamageReport r = inv.getArgument(0);
            if (r.getId() == null) {
                r.setId(1L);
            }
            return r;
        });

        DamageReportResponse resp = damageReportService.createReport(request("剑柄缠绳断裂处", "场务小李"));

        assertEquals("BS000001", resp.getReportNo());
        assertEquals("剑柄缠绳断裂处", resp.getDamagedPart());
        assertEquals("场务小李", resp.getDiscoverer());
        assertEquals(PropDamageReport.STATUS_OPEN, resp.getStatus());
        assertEquals("损坏", prop.getStatus());
        verify(propRepository).save(prop);
    }

    @Test
    void 未结案时不许再开第二张() {
        when(propRepository.findById(5L)).thenReturn(Optional.of(prop));
        when(damageReportRepository.findFirstByPropIdAndStatus(5L, PropDamageReport.STATUS_OPEN))
                .thenReturn(Optional.of(openReport()));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> damageReportService.createReport(request("剑刃缺口", "小王")));
        assertTrue(ex.getMessage().contains("未结案"));
        assertTrue(ex.getMessage().contains("BS000001"));
        verify(damageReportRepository, never()).saveAndFlush(any());
    }

    @Test
    void 损坏部位和发现人必填() {
        when(propRepository.findById(5L)).thenReturn(Optional.of(prop));
        assertThrows(BusinessException.class,
                () -> damageReportService.createReport(request("  ", "场务小李")));
        assertThrows(BusinessException.class,
                () -> damageReportService.createReport(request("剑柄", " ")));
        verify(damageReportRepository, never()).saveAndFlush(any());
    }

    @Test
    void 结案后才能再开新单_重复结案被拦截() {
        PropDamageReport report = openReport();
        when(damageReportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(damageReportRepository.save(any(PropDamageReport.class))).thenAnswer(inv -> inv.getArgument(0));
        when(propRepository.findById(5L)).thenReturn(Optional.of(prop));

        DamageReportResponse closed = damageReportService.closeReport(1L);
        assertEquals(PropDamageReport.STATUS_CLOSED, closed.getStatus());
        assertNotNull(closed.getClosedAt());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> damageReportService.closeReport(1L));
        assertTrue(ex.getMessage().contains("已结案"));
    }

    @Test
    void 打开报损单仍看到损坏部位和发现人原文() {
        when(damageReportRepository.findById(1L)).thenReturn(Optional.of(openReport()));
        when(propRepository.findById(5L)).thenReturn(Optional.of(prop));

        DamageReportResponse resp = damageReportService.getReportById(1L);
        assertEquals("剑柄缠绳断裂处", resp.getDamagedPart());
        assertEquals("场务小李", resp.getDiscoverer());
    }
}
