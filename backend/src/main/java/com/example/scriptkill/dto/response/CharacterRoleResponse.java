package com.example.scriptkill.dto.response;

import com.example.scriptkill.entity.CharacterRole;
import com.example.scriptkill.entity.ScriptTheme;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 人物角色响应：所属剧本名不存冗余字段，读取时按 scriptThemeId 实时关联当前主题，
 * 保证剧本改名后，人物列表刷新即显示新名，不会挂着旧名。
 */
@Data
public class CharacterRoleResponse {

    private Long id;
    private String roleName;
    private Long scriptThemeId;

    /** 所属剧本名：读取时实时取当前主题名 */
    private String themeName;

    private String gender;
    private Integer age;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CharacterRoleResponse fromEntity(CharacterRole role, ScriptTheme theme) {
        CharacterRoleResponse response = new CharacterRoleResponse();
        response.setId(role.getId());
        response.setRoleName(role.getRoleName());
        response.setScriptThemeId(role.getScriptThemeId());
        response.setThemeName(theme != null ? theme.getThemeName() : null);
        response.setGender(role.getGender());
        response.setAge(role.getAge());
        response.setDescription(role.getDescription());
        response.setCreatedAt(role.getCreatedAt());
        response.setUpdatedAt(role.getUpdatedAt());
        return response;
    }
}
