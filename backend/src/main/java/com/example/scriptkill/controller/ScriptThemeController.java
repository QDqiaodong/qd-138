package com.example.scriptkill.controller;

import com.example.scriptkill.dto.response.ApiResponse;
import com.example.scriptkill.dto.response.ScriptThemeDetailResponse;
import com.example.scriptkill.entity.ScriptTheme;
import com.example.scriptkill.repository.ScriptThemeRepository;
import com.example.scriptkill.service.RolePropService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/theme")
@CrossOrigin(origins = "*")
public class ScriptThemeController {

    @Autowired
    private ScriptThemeRepository scriptThemeRepository;

    @Autowired
    private RolePropService rolePropService;

    @GetMapping
    public ApiResponse<List<ScriptTheme>> getAllThemes() {
        return ApiResponse.success(scriptThemeRepository.findAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<ScriptTheme> getThemeById(@PathVariable Long id) {
        return ApiResponse.success(scriptThemeRepository.findById(id).orElse(null));
    }

    @GetMapping("/{id}/detail")
    public ApiResponse<ScriptThemeDetailResponse> getThemeDetail(@PathVariable Long id) {
        return ApiResponse.success(rolePropService.getThemeDetail(id));
    }

    @PostMapping
    public ApiResponse<ScriptTheme> createTheme(@RequestBody ScriptTheme theme) {
        return ApiResponse.success(scriptThemeRepository.save(theme));
    }

    @PutMapping("/{id}")
    public ApiResponse<ScriptTheme> updateTheme(@PathVariable Long id, @RequestBody ScriptTheme theme) {
        ScriptTheme existing = scriptThemeRepository.findById(id).orElse(null);
        if (existing != null) {
            existing.setThemeName(theme.getThemeName());
            existing.setDescription(theme.getDescription());
            existing.setEra(theme.getEra());
            existing.setDifficulty(theme.getDifficulty());
            return ApiResponse.success(scriptThemeRepository.save(existing));
        }
        return ApiResponse.error(404, "剧本主题不存在");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteTheme(@PathVariable Long id) {
        scriptThemeRepository.deleteById(id);
        return ApiResponse.success(null);
    }
}