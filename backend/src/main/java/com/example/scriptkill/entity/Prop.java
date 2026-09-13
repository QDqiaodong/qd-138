package com.example.scriptkill.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "prop")
@Data
public class Prop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "prop_code", nullable = false, length = 50)
    private String propCode;

    @Column(name = "prop_name", nullable = false, length = 100)
    private String propName;

    @Column(length = 50)
    private String era;

    @Column(name = "prop_type", length = 50)
    private String propType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 20)
    private String status = "正常";

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