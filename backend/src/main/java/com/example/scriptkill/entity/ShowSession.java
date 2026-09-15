package com.example.scriptkill.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "show_session")
@Data
public class ShowSession {

    /** 排班中：主题里的人物还没排完或尚未确认排好 */
    public static final String STATUS_SCHEDULING = "排班中";

    /** 已排好：主题里的全部人物都已指定演职人员，并经场务确认 */
    public static final String STATUS_READY = "已排好";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_no", length = 30)
    private String sessionNo;

    @Column(name = "script_theme_id", nullable = false)
    private Long scriptThemeId;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(length = 20)
    private String status = STATUS_SCHEDULING;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
