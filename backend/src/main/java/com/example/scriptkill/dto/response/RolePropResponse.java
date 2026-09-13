package com.example.scriptkill.dto.response;

import com.example.scriptkill.entity.Prop;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RolePropResponse {
    private Long id;
    private Long characterRoleId;
    private String roleName;
    private Long scriptThemeId;
    private String themeName;
    private Long propId;
    private String propCode;
    private String propName;
    private String propType;
    private String era;
    private LocalDateTime bindTime;

    public static RolePropResponse fromEntity(com.example.scriptkill.entity.RoleProp roleProp, 
                                               com.example.scriptkill.entity.CharacterRole role,
                                               com.example.scriptkill.entity.ScriptTheme theme,
                                               Prop prop) {
        RolePropResponse response = new RolePropResponse();
        response.setId(roleProp.getId());
        response.setCharacterRoleId(roleProp.getCharacterRoleId());
        response.setRoleName(role != null ? role.getRoleName() : null);
        response.setScriptThemeId(roleProp.getScriptThemeId());
        response.setThemeName(theme != null ? theme.getThemeName() : null);
        response.setPropId(roleProp.getPropId());
        response.setPropCode(prop != null ? prop.getPropCode() : null);
        response.setPropName(prop != null ? prop.getPropName() : null);
        response.setPropType(prop != null ? prop.getPropType() : null);
        response.setEra(prop != null ? prop.getEra() : null);
        response.setBindTime(roleProp.getBindTime());
        return response;
    }
}