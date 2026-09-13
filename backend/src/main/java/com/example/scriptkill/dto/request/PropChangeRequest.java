package com.example.scriptkill.dto.request;

import lombok.Data;

@Data
public class PropChangeRequest {
    private Long propId;
    private Long fromRoleId;
    private Long toRoleId;
    private Long scriptThemeId;
    private String operator;
    private String reason;
}