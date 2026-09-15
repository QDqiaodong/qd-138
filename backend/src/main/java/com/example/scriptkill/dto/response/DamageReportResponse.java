package com.example.scriptkill.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 报损单响应：损坏部位与发现人按开单时录入的原文返回，
 * 场务任何时候打开报损单都能看到这两项原始内容。
 */
@Data
public class DamageReportResponse {

    private Long id;

    /** 报损单号 */
    private String reportNo;

    private Long propId;
    private String propCode;
    private String propName;

    /** 损坏部位（开单时录入的原文，不可修改） */
    private String damagedPart;

    /** 发现人（开单时录入的原文，不可修改） */
    private String discoverer;

    /** 状态：未结案/已结案 */
    private String status;

    private LocalDateTime createdAt;
    private LocalDateTime closedAt;
}
