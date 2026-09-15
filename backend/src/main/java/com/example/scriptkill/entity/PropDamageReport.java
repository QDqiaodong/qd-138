package com.example.scriptkill.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "prop_damage_report")
@Data
public class PropDamageReport {

    /** 未结案：此时该道具不允许再开第二张报损单 */
    public static final String STATUS_OPEN = "未结案";

    /** 已结案：结案后才允许就同一道具再开新单 */
    public static final String STATUS_CLOSED = "已结案";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_no", length = 30)
    private String reportNo;

    @Column(name = "prop_id", nullable = false)
    private Long propId;

    @Column(name = "damaged_part", nullable = false, length = 200)
    private String damagedPart;

    @Column(name = "discoverer", nullable = false, length = 100)
    private String discoverer;

    @Column(length = 20)
    private String status = STATUS_OPEN;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
