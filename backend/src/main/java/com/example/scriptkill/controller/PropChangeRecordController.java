package com.example.scriptkill.controller;

import com.example.scriptkill.dto.response.ApiResponse;
import com.example.scriptkill.dto.response.CharacterReviewResponse;
import com.example.scriptkill.entity.PropChangeRecord;
import com.example.scriptkill.repository.PropChangeRecordRepository;
import com.example.scriptkill.service.CharacterReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/change-record")
@CrossOrigin(origins = "*")
public class PropChangeRecordController {

    @Autowired
    private PropChangeRecordRepository changeRecordRepository;

    @Autowired
    private CharacterReviewService characterReviewService;

    @GetMapping
    public ApiResponse<List<PropChangeRecord>> getAllRecords() {
        return ApiResponse.success(changeRecordRepository.findAll());
    }

    @GetMapping("/character-review")
    public ApiResponse<CharacterReviewResponse> getCharacterReview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.success(characterReviewService.getCharacterReview(date));
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