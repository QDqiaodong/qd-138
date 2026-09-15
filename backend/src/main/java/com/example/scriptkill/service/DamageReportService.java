package com.example.scriptkill.service;

import com.example.scriptkill.dto.request.DamageReportCreateRequest;
import com.example.scriptkill.dto.response.DamageReportResponse;

import java.util.List;

public interface DamageReportService {
    /**
     * 全部报损单，按开单时间倒序。
     */
    List<DamageReportResponse> getAllReports();

    /**
     * 打开一张报损单：损坏部位与发现人返回开单时录入的原文。
     */
    DamageReportResponse getReportById(Long id);

    /**
     * 开报损单：选一件现有道具，录入损坏部位和发现人。
     * 该道具存在未结案报损单时不许再开第二张；开单后道具档案状态置为「损坏」。
     */
    DamageReportResponse createReport(DamageReportCreateRequest request);

    /**
     * 结案：只有结案后该道具才允许再开新单。
     */
    DamageReportResponse closeReport(Long id);
}
