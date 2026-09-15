package com.example.scriptkill.controller;

import com.example.scriptkill.dto.response.ApiResponse;
import com.example.scriptkill.dto.response.CharacterRoleResponse;
import com.example.scriptkill.entity.CharacterRole;
import com.example.scriptkill.repository.CharacterRoleRepository;
import com.example.scriptkill.service.CharacterRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/role")
@CrossOrigin(origins = "*")
public class CharacterRoleController {

    @Autowired
    private CharacterRoleRepository characterRoleRepository;

    @Autowired
    private CharacterRoleService characterRoleService;

    @GetMapping
    public ApiResponse<List<CharacterRoleResponse>> getAllRoles() {
        return ApiResponse.success(characterRoleService.listAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<CharacterRole> getRoleById(@PathVariable Long id) {
        return ApiResponse.success(characterRoleRepository.findById(id).orElse(null));
    }

    @GetMapping("/theme/{themeId}")
    public ApiResponse<List<CharacterRoleResponse>> getRolesByTheme(@PathVariable Long themeId) {
        return ApiResponse.success(characterRoleService.listByTheme(themeId));
    }

    @PostMapping
    public ApiResponse<CharacterRole> createRole(@RequestBody CharacterRole role) {
        return ApiResponse.success(characterRoleRepository.save(role));
    }

    @PutMapping("/{id}")
    public ApiResponse<CharacterRole> updateRole(@PathVariable Long id, @RequestBody CharacterRole role) {
        CharacterRole existing = characterRoleRepository.findById(id).orElse(null);
        if (existing != null) {
            existing.setRoleName(role.getRoleName());
            existing.setScriptThemeId(role.getScriptThemeId());
            existing.setGender(role.getGender());
            existing.setAge(role.getAge());
            existing.setDescription(role.getDescription());
            return ApiResponse.success(characterRoleRepository.save(existing));
        }
        return ApiResponse.error(404, "角色不存在");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteRole(@PathVariable Long id) {
        characterRoleService.delete(id);
        return ApiResponse.success(null);
    }

    // 与默认删除分开：只有场务显式走「撤下并删除」，才会先撤各场排班再连人带档拿掉
    @PostMapping("/{id}/unassign-and-delete")
    public ApiResponse<Void> unassignAndDeleteRole(@PathVariable Long id) {
        characterRoleService.unassignAndDelete(id);
        return ApiResponse.success(null);
    }
}