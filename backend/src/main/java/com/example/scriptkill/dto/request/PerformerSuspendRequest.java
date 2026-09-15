package com.example.scriptkill.dto.request;

import lombok.Data;

@Data
public class PerformerSuspendRequest {
    /** 停演原因，必填，按录入原文落库 */
    private String reason;
}
