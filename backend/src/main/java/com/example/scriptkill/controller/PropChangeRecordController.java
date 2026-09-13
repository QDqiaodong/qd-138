package com.example.scriptkill.controller;

import com.example.scriptkill.dto.response.ApiResponse;
import com.example.scriptkill.entity.PropChangeRecord;
import com.example.scriptkill.repository.PropChangeRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/change-record")
@CrossOrigin(origins = "*")
public class PropChangeRecordController {

    @Autowired
    private PropChangeRecordRepository changeRecordRepository;

    @GetMapping
    public ApiResponse<List<PropChangeRecord>> getAllRecords() {
        return ApiResponse.success(changeRecordRepository.findAll());
    }

    @GetMapping("/prop/{propId}")
    public ApiResponse<List<PropChangeRecord>> getRecordsByPropId(@PathVariable Long propId) {
        return ApiResponse.success(changeRecordRepository.findByPropIdOrderByCreatedAtDesc(propId));
    }

    @GetMapping("/role/{roleId}")
    public ApiResponse<List<PropChangeRecord>> getRecordsByRoleId(@PathVariable Long roleId) {
        return ApiResponse.success(changeRecordRepository.findByCharacterRoleIdOrderByCreatedAtDesc(roleId));
    }

    @GetMapping("/theme/{themeId}")
    public ApiResponse<List<PropChangeRecord>> getRecordsByThemeId(@PathVariable Long themeId) {
        return ApiResponse.success(changeRecordRepository.findByScriptThemeIdOrderByCreatedAtDesc(themeId));
    }
}