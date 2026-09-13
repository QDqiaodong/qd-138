package com.example.scriptkill.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "role_prop")
@Data
public class RoleProp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "character_role_id", nullable = false)
    private Long characterRoleId;

    @Column(name = "prop_id", nullable = false)
    private Long propId;

    @Column(name = "script_theme_id", nullable = false)
    private Long scriptThemeId;

    @Column(name = "bind_time")
    private LocalDateTime bindTime;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        bindTime = LocalDateTime.now();
        createdAt = LocalDateTime.now();
    }
}