package com.voxnovel.core_content_service.dto.response;

import lombok.Data;

@Data
public class NovelCharacterResponse {
    private Long id;
    private Long novelId;
    private String name;
    private String gender;
    private String description;
    private Long defaultVoiceId;
    private String defaultVoiceName; // Trả về tên giọng để hiển thị UI
}