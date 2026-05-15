package com.voxnovel.core_content_service.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "voices")
@Data
public class Voice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // LIÊN KẾT MỚI: Trỏ trực tiếp tới bảng Provider
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private Provider provider;

    @Column(nullable = false)
    private String providerVoiceId; // ID giọng đọc của nhà cung cấp (vd: alloy, echo)

    @Column(nullable = false)
    private String name; // Tên hiển thị cho Admin chọn (vd: Giọng nam trầm)

    private String gender; // MALE, FEMALE
    private String language; // Ngôn ngữ hỗ trợ (vd: vi-VN)
    private String previewUrl; // Link mp3 nghe thử
}
