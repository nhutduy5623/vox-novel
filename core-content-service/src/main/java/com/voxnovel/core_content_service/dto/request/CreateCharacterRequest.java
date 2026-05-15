package com.voxnovel.core_content_service.dto.request;

import lombok.Data;

@Data
public class CreateCharacterRequest {
    private Long novelId;
    private String name;
    private String gender;
    private String description;
    private Long defaultVoiceId; // Có thể null nếu chưa muốn gán giọng ngay

}
