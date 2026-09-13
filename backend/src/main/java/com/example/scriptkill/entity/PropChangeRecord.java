package com.example.scriptkill.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "prop_change_record")
@Data
public class PropChangeRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "prop_id", nullable = false)
    private Long propId;

    @Column(name = "character_role_id")
    private Long characterRoleId;

    @Column(name = "script_theme_id")
    private Long scriptThemeId;

    @Column(name = "change_type", nullable = false, length = 20)
    private String changeType;

    @Column(name = "before_value", columnDefinition = "TEXT")
    private String beforeValue;

    @Column(name = "after_value", columnDefinition = "TEXT")
    private String afterValue;

    @Column(name = "change_reason", length = 500)
    private String changeReason;

    @Column(length = 100)
    private String operator;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}