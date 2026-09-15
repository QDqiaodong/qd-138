package com.example.scriptkill.service.impl;

import com.example.scriptkill.dto.request.DamageReportCreateRequest;
import com.example.scriptkill.dto.response.DamageReportResponse;
import com.example.scriptkill.entity.Prop;
import com.example.scriptkill.entity.PropDamageReport;
import com.example.scriptkill.exception.BusinessException;
import com.example.scriptkill.repository.PropDamageReportRepository;
import com.example.scriptkill.repository.PropRepository;
import com.example.scriptkill.service.DamageReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class DamageReportServiceImpl implements DamageReportService {

    @Autowired
    private PropDamageReportRepository damageReportRepository;

    @Autowired
    private PropRepository propRepository;

    @Override
    public List<DamageReportResponse> getAllReports() {
        List<PropDamageReport> reports = damageReportRepository.findAllByOrderByCreatedAtDesc();
        List<DamageReportResponse> responses = new ArrayList<>();
        for (PropDamageReport report : reports) {
            responses.add(toResponse(report));
        }
        return responses;
    }

    @Override
    public DamageReportResponse getReportById(Long id) {
        PropDamageReport report = damageReportRepository.findById(id)
                .orElseThrow(() -> new BusinessException("报损单不存在，请刷新后重试"));
        return toResponse(report);
    }

    @Override
    @Transactional
    public DamageReportResponse createReport(DamageReportCreateRequest request) {
        if (request.getPropId() == null) {
            throw new BusinessException("请选择要报损的道具");
        }
        Prop prop = propRepository.findById(request.getPropId())
                .orElseThrow(() -> new BusinessException("所选道具不存在，请刷新道具列表后重选"));

        String damagedPart = trim(request.getDamagedPart());
        if (damagedPart.isEmpty()) {
            throw new BusinessException("请填写损坏部位");
        }
        String discoverer = trim(request.getDiscoverer());
        if (discoverer.isEmpty()) {
            throw new BusinessException("请填写发现人");
        }

        // 同一道具存在未结案报损单时不许再开第二张，先结案才能再开新单
        damageReportRepository.findFirstByPropIdAndStatus(prop.getId(), PropDamageReport.STATUS_OPEN)
                .ifPresent(open -> {
                    throw new BusinessException("道具「" + prop.getPropCode() + " " + prop.getPropName()
                            + "」已有未结案的报损单（单号 " + open.getReportNo() + "），请先结案后再开新单");
                });

        PropDamageReport report = new PropDamageReport();
        report.setPropId(prop.getId());
        report.setDamagedPart(damagedPart);
        report.setDiscoverer(discoverer);
        report.setStatus(PropDamageReport.STATUS_OPEN);
        try {
            // 落库拿到自增ID后生成单号；若数据库唯一约束拦下重复未结案单，转成业务提示
            PropDamageReport saved = damageReportRepository.saveAndFlush(report);
            saved.setReportNo(String.format("BS%06d", saved.getId()));
            damageReportRepository.saveAndFlush(saved);

            // 开单即把道具档案状态置为「损坏」，与场务改档案的动作保持一致
            prop.setStatus("损坏");
            propRepository.save(prop);
            return toResponse(saved);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("道具「" + prop.getPropCode() + " " + prop.getPropName()
                    + "」已有未结案的报损单，请先结案后再开新单");
        }
    }

    @Override
    @Transactional
    public DamageReportResponse closeReport(Long id) {
        PropDamageReport report = damageReportRepository.findById(id)
                .orElseThrow(() -> new BusinessException("报损单不存在，请刷新后重试"));
        if (PropDamageReport.STATUS_CLOSED.equals(report.getStatus())) {
            throw new BusinessException("报损单「" + report.getReportNo() + "」已结案，请勿重复操作");
        }
        report.setStatus(PropDamageReport.STATUS_CLOSED);
        report.setClosedAt(LocalDateTime.now());
        return toResponse(damageReportRepository.save(report));
    }

    /**
     * 损坏部位与发现人按开单时录入的原文返回，任何环节都不改写。
     */
    private DamageReportResponse toResponse(PropDamageReport report) {
        Prop prop = propRepository.findById(report.getPropId()).orElse(null);
        DamageReportResponse response = new DamageReportResponse();
        response.setId(report.getId());
        response.setReportNo(report.getReportNo());
        response.setPropId(report.getPropId());
        response.setPropCode(prop != null ? prop.getPropCode() : "");
        response.setPropName(prop != null ? prop.getPropName() : "");
        response.setDamagedPart(report.getDamagedPart());
        response.setDiscoverer(report.getDiscoverer());
        response.setStatus(report.getStatus());
        response.setCreatedAt(report.getCreatedAt());
        response.setClosedAt(report.getClosedAt());
        return response;
    }

    private String trim(String value) {
        return value != null ? value.trim() : "";
    }
}
