package com.example.scriptkill.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class PropBindRequest {
    private Long characterRoleId;
    private Long scriptThemeId;
    private List<Long> propIds;
    private String operator;
    private String reason;
}