package com.voxnovel.core_content_service.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "novel_characters")
@Data
public class NovelCharacter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "novel_id", nullable = false)
    private Novel novel; // Thuộc truyện nào

    @Column(nullable = false)
    private String name; // Tên nhân vật

    private String gender; // Giới tính (MALE, FEMALE, UNKNOWN)

    @Column(columnDefinition = "TEXT")
    private String description; // Mô tả tính cách để đưa cho AI làm ngữ cảnh

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_voice_id")
    private Voice defaultVoice; // Giọng mặc định được gán cho nhân vật này
}