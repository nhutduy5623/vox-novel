package com.voxnovel.core_content_service.entity;

import com.voxnovel.core_content_service.enums.ProviderType;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "providers")
@Data
public class Provider {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code; // Mã định danh (VD: OPENAI, GEMINI, ELEVENLABS)

    @Column(nullable = false)
    private String name; // Tên hiển thị (VD: OpenAI, Google Gemini)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProviderType type; // Chức năng: LLM hoặc TTS
}
