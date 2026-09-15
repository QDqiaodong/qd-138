package com.example.scriptkill.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 人物回看响应：回看名单（按人物汇总）与当天明细同源返回，
 * 场务刷新后两侧数据始终一致。
 */
@Data
public class CharacterReviewResponse {

    /** 回看日期（yyyy-MM-dd） */
    private String date;

    /** 当天有道具变更的人物数 */
    private int roleCount;

    /** 当天变更总条数 */
    private int totalCount;

    /** 当天未确认变更总条数（未填写变更原因） */
    private int unconfirmedCount;

    /** 回看名单：按人物汇总当天变更 */
    private List<RoleSummary> summary;

    /** 当天明细：当天全部变更记录 */
    private List<ReviewRecord> details;

    @Data
    public static class RoleSummary {
        private Long roleId;
        private String roleName;
        private String themeName;
        private int bindCount;
        private int unbindCount;
        private int changeCount;
        private int totalCount;
        private int unconfirmedCount;
    }

    @Data
    public static class ReviewRecord {
        private Long id;
        private Long propId;
        private String propCode;
        private String propName;
        private Long roleId;
        private String roleName;
        private String themeName;
        private String changeType;
        private String beforeValue;
        private String afterValue;
        private String changeReason;
        /** 未填写变更原因时为 true，场务需重点核对 */
        private boolean unconfirmed;
        private String operator;
        private LocalDateTime createdAt;
    }
}
