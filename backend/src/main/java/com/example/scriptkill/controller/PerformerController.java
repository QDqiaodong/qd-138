package com.example.scriptkill.controller;

import com.example.scriptkill.dto.request.PerformerCreateRequest;
import com.example.scriptkill.dto.response.ApiResponse;
import com.example.scriptkill.entity.Performer;
import com.example.scriptkill.service.PerformerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/performer")
@CrossOrigin(origins = "*")
public class PerformerController {

    @Autowired
    private PerformerService performerService;

    @GetMapping
    public ApiResponse<List<Performer>> getAllPerformers() {
        return ApiResponse.success(performerService.listAll());
    }

    @PostMapping
    public ApiResponse<Performer> createPerformer(@RequestBody PerformerCreateRequest request) {
        return ApiResponse.success(performerService.create(request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deletePerformer(@PathVariable Long id) {
        performerService.delete(id);
        return ApiResponse.success(null);
    }
}
