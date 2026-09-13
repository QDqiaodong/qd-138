package com.example.scriptkill.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class ScriptThemeDetailResponse {
    private Long id;
    private String themeName;
    private String description;
    private String era;
    private String difficulty;
    private List<RoleDetail> roles;

    @Data
    public static class RoleDetail {
        private Long id;
        private String roleName;
        private String gender;
        private Integer age;
        private String description;
        private List<PropDetail> props;
    }

    @Data
    public static class PropDetail {
        private Long id;
        private String propCode;
        private String propName;
        private String propType;
        private String era;
        private String status;
    }
}