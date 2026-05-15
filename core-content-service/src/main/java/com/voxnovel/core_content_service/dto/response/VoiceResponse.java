package com.voxnovel.core_content_service.dto.response;

import lombok.Data;

@Data
public class VoiceResponse {
    private Long id;
    private Long providerId;
    private String providerCode; // VD: ELEVENLABS
    private String providerVoiceId;
    private String name;
    private String gender;
    private String language;
    private String previewUrl;
}
