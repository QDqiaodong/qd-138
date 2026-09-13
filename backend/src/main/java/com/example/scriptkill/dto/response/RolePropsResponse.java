package com.example.scriptkill.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class RolePropsResponse {
    private Long roleId;
    private String roleName;
    private Long scriptThemeId;
    private String themeName;
    private List<RolePropsResponse.PropDetail> props;

    @Data
    public static class PropDetail {
        private Long id;
        private String propCode;
        private String propName;
        private String propType;
        private String era;
        private String status;
        private String description;
    }
}