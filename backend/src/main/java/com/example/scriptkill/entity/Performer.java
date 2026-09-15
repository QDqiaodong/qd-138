package com.example.scriptkill.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "performer")
@Data
public class Performer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "performer_name", nullable = false, length = 100)
    private String performerName;

    /** 是否停演：标停后未开演场次的名单里此人标为待换，且挡住开演 */
    @Column(nullable = false)
    private Boolean suspended = false;

    /** 停演原因：标停演时录入的原文，拦截开演时原样写出 */
    @Column(name = "suspend_reason", length = 500)
    private String suspendReason;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
