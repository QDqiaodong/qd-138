package com.example.scriptkill.controller;

import com.example.scriptkill.dto.request.DamageReportCreateRequest;
import com.example.scriptkill.dto.response.ApiResponse;
import com.example.scriptkill.dto.response.DamageReportResponse;
import com.example.scriptkill.service.DamageReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/damage-report")
@CrossOrigin(origins = "*")
public class DamageReportController {

    @Autowired
    private DamageReportService damageReportService;

    @GetMapping
    public ApiResponse<List<DamageReportResponse>> getAllReports() {
        return ApiResponse.success(damageReportService.getAllReports());
    }

    @GetMapping("/{id}")
    public ApiResponse<DamageReportResponse> getReportById(@PathVariable Long id) {
        return ApiResponse.success(damageReportService.getReportById(id));
    }

    @PostMapping
    public ApiResponse<DamageReportResponse> createReport(@RequestBody DamageReportCreateRequest request) {
        return ApiResponse.success(damageReportService.createReport(request));
    }

    @PostMapping("/{id}/close")
    public ApiResponse<DamageReportResponse> closeReport(@PathVariable Long id) {
        return ApiResponse.success(damageReportService.closeReport(id));
    }
}
