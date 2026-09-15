package com.example.scriptkill.dto.request;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SessionCreateRequest {
    private Long scriptThemeId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
