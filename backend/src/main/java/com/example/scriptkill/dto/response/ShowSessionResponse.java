package com.example.scriptkill.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ShowSessionResponse {

    private Long id;
    private String sessionNo;
    private Long scriptThemeId;
    /** 读取时实时关联的当前主题名，主题改名后刷新即为新名 */
    private String themeName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    /** 该主题下人物总数 */
    private int totalRoles;
    /** 已排上演职人员的人物数 */
    private int assignedCount;
    private List<AssignmentView> assignments;

    @Data
    public static class AssignmentView {
        private Long characterRoleId;
        private String roleName;
        private Long performerId;
        private String performerName;
    }
}
