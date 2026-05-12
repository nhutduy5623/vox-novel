package com.voxnovel.media_tts_service.dto.response;

import lombok.Data;

@Data
public class AudioLineResult {
    private int sequence;
    private String characterId;
    private String audioUrl; // Link MinIO chứa file MP3 nhỏ
    private String status;   // SUCCESS hoặc FAILED để Core dễ xử lý
}
