package com.example.scriptkill.controller;

import com.example.scriptkill.dto.request.PropBindRequest;
import com.example.scriptkill.dto.request.PropChangeRequest;
import com.example.scriptkill.dto.response.ApiResponse;
import com.example.scriptkill.dto.response.RolePropsResponse;
import com.example.scriptkill.dto.response.RolePropResponse;
import com.example.scriptkill.service.RolePropService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/role-prop")
@CrossOrigin(origins = "*")
public class RolePropController {

    @Autowired
    private RolePropService rolePropService;

    @GetMapping
    public ApiResponse<List<RolePropResponse>> getAllRoleProps() {
        return ApiResponse.success(rolePropService.getAllRoleProps());
    }

    @GetMapping("/role/{roleId}")
    public ApiResponse<List<RolePropResponse>> getRolePropsByRoleId(@PathVariable Long roleId) {
        return ApiResponse.success(rolePropService.getRolePropsByRoleId(roleId));
    }

    @GetMapping("/prop/{propId}")
    public ApiResponse<List<RolePropResponse>> getRolePropsByPropId(@PathVariable Long propId) {
        return ApiResponse.success(rolePropService.getRolePropsByPropId(propId));
    }

    @GetMapping("/theme/{themeId}")
    public ApiResponse<List<RolePropResponse>> getRolePropsByThemeId(@PathVariable Long themeId) {
        return ApiResponse.success(rolePropService.getRolePropsByThemeId(themeId));
    }

    @GetMapping("/search-by-role-name")
    public ApiResponse<RolePropsResponse> searchByRoleName(@RequestParam String roleName) {
        return ApiResponse.success(rolePropService.getRolePropsByRoleName(roleName));
    }

    @PostMapping("/bind")
    public ApiResponse<Void> bindPropsToRole(@RequestBody PropBindRequest request) {
        rolePropService.bindPropsToRole(request);
        return ApiResponse.success("绑定成功", null);
    }

    @PostMapping("/unbind")
    public ApiResponse<Void> unbindPropsFromRole(@RequestBody Map<String, Object> request) {
        Long roleId = ((Number) request.get("roleId")).longValue();
        @SuppressWarnings("unchecked")
        List<Long> propIds = ((List<Number>) request.get("propIds")).stream()
                .map(Number::longValue)
                .toList();
        rolePropService.unbindPropsFromRole(roleId, propIds);
        return ApiResponse.success("解绑成功", null);
    }

    @PostMapping("/change")
    public ApiResponse<Void> changePropRole(@RequestBody PropChangeRequest request) {
        rolePropService.changePropRole(request);
        return ApiResponse.success("更换成功", null);
    }
}