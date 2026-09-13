package com.example.scriptkill.service;

import com.example.scriptkill.dto.request.PropBindRequest;
import com.example.scriptkill.dto.request.PropChangeRequest;
import com.example.scriptkill.dto.response.RolePropsResponse;
import com.example.scriptkill.dto.response.RolePropResponse;
import com.example.scriptkill.dto.response.ScriptThemeDetailResponse;

import java.util.List;

public interface RolePropService {
    List<RolePropResponse> getAllRoleProps();
    List<RolePropResponse> getRolePropsByRoleId(Long roleId);
    List<RolePropResponse> getRolePropsByPropId(Long propId);
    List<RolePropResponse> getRolePropsByThemeId(Long themeId);
    RolePropsResponse getRolePropsByRoleName(String roleName);
    ScriptThemeDetailResponse getThemeDetail(Long themeId);
    void bindPropsToRole(PropBindRequest request);
    void unbindPropsFromRole(Long roleId, List<Long> propIds);
    void changePropRole(PropChangeRequest request);
}