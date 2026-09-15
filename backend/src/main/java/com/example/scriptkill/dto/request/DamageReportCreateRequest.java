package com.example.scriptkill.dto.request;

import lombok.Data;

@Data
public class DamageReportCreateRequest {
    private Long propId;
    private String damagedPart;
    private String discoverer;
}
