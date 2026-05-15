package com.voxnovel.core_content_service.entity;

import com.voxnovel.core_content_service.dto.message.ScriptLineDto;
import com.voxnovel.core_content_service.enums.ChapterStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "chapters")
@Data
public class Chapter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "novel_id", nullable = false)
    private Novel novel;

    private Integer chapterNumber; // Số thứ tự chương

    private String title; // Tên chương

    @Column(columnDefinition = "TEXT", nullable = false)
    private String originalContent; // Text gốc do người dùng nhập vào

    // Ánh xạ trực tiếp JSON vào List, không cần dùng String
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private List<ScriptLineDto> scriptData;

    // Thêm trường này để lưu danh sách ID diễn viên
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private List<Long> characterIds;

    private String audioUrl; // URL siêu phẩm Mp3 sau khi Media ghép xong

    private Boolean hasAudio = false; // Đánh dấu đã có audio hay chưa

    @Enumerated(EnumType.STRING)
    private ChapterStatus status; // Enum: DRAFT, PROCESSING_AI, REVIEWING, PUBLISHED

    @Column(nullable = false, updatable = false)
    private String createdBy; // userId

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}