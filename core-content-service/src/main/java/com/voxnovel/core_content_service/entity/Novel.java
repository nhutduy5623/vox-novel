package com.voxnovel.core_content_service.entity;

import com.voxnovel.core_content_service.enums.NovelStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "novels")
@Data
public class Novel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title; // Tên truyện

    @Column(columnDefinition = "TEXT")
    private String description; // Mô tả truyện

    private String coverImageUrl; // Link ảnh bìa

    @Enumerated(EnumType.STRING)
    private NovelStatus status; // Enum: DRAFT, PUBLISHED

    @Column(nullable = false, updatable = false)
    private String createdBy; // userId từ Token truyền xuống

    // Các trường audit (Ngày tạo, ngày cập nhật)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
