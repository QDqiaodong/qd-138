package com.example.scriptkill.controller;

import com.example.scriptkill.dto.response.ApiResponse;
import com.example.scriptkill.entity.Prop;
import com.example.scriptkill.service.PropService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/prop")
@CrossOrigin(origins = "*")
public class PropController {

    @Autowired
    private PropService propService;

    @GetMapping
    public ApiResponse<List<Prop>> getAllProps() {
        return ApiResponse.success(propService.getAllProps());
    }

    @GetMapping("/{id}")
    public ApiResponse<Prop> getPropById(@PathVariable Long id) {
        return ApiResponse.success(propService.getPropById(id));
    }

    @GetMapping("/code/{code}")
    public ApiResponse<Prop> getPropByCode(@PathVariable String code) {
        return ApiResponse.success(propService.getPropByCode(code));
    }

    @GetMapping("/era/{era}")
    public ApiResponse<List<Prop>> getPropsByEra(@PathVariable String era) {
        return ApiResponse.success(propService.getPropsByEra(era));
    }

    @GetMapping("/type/{type}")
    public ApiResponse<List<Prop>> getPropsByType(@PathVariable String type) {
        return ApiResponse.success(propService.getPropsByType(type));
    }

    @GetMapping("/era-style-template")
    public ApiResponse<Map<String, List<String>>> getEraStyleTemplate() {
        return ApiResponse.success(propService.getEraStyleTemplate());
    }

    @PostMapping
    public ApiResponse<Prop> createProp(@RequestBody Prop prop) {
        return ApiResponse.success(propService.createProp(prop));
    }

    @PutMapping("/{id}")
    public ApiResponse<Prop> updateProp(@PathVariable Long id, @RequestBody Prop prop) {
        Prop updated = propService.updateProp(id, prop);
        if (updated != null) {
            return ApiResponse.success(updated);
        }
        return ApiResponse.error(404, "道具不存在");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteProp(@PathVariable Long id) {
        propService.deleteProp(id);
        return ApiResponse.success(null);
    }
}