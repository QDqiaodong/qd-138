package com.example.scriptkill.controller;

import com.example.scriptkill.dto.request.SessionAssignRequest;
import com.example.scriptkill.dto.request.SessionCreateRequest;
import com.example.scriptkill.dto.response.ApiResponse;
import com.example.scriptkill.dto.response.ShowSessionResponse;
import com.example.scriptkill.service.ShowSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/session")
@CrossOrigin(origins = "*")
public class ShowSessionController {

    @Autowired
    private ShowSessionService showSessionService;

    @GetMapping
    public ApiResponse<List<ShowSessionResponse>> getAllSessions() {
        return ApiResponse.success(showSessionService.listSessions());
    }

    @GetMapping("/{id}")
    public ApiResponse<ShowSessionResponse> getSessionById(@PathVariable Long id) {
        return ApiResponse.success(showSessionService.getSession(id));
    }

    @PostMapping
    public ApiResponse<ShowSessionResponse> createSession(@RequestBody SessionCreateRequest request) {
        return ApiResponse.success(showSessionService.createSession(request));
    }

    @PostMapping("/{id}/assign")
    public ApiResponse<ShowSessionResponse> assign(@PathVariable Long id, @RequestBody SessionAssignRequest request) {
        return ApiResponse.success(showSessionService.assign(id, request));
    }

    @PostMapping("/{id}/unassign/{characterRoleId}")
    public ApiResponse<ShowSessionResponse> unassign(@PathVariable Long id, @PathVariable Long characterRoleId) {
        return ApiResponse.success(showSessionService.unassign(id, characterRoleId));
    }

    @PostMapping("/{id}/ready")
    public ApiResponse<ShowSessionResponse> markReady(@PathVariable Long id) {
        return ApiResponse.success(showSessionService.markReady(id));
    }
}
