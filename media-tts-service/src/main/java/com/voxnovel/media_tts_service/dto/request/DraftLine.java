package com.voxnovel.media_tts_service.dto.request;

import lombok.Data;

@Data
public class DraftLine {
    private int sequence;
    private String characterId;
    private String voiceId; // Mã giọng đọc bên thứ 3 (FPT, Viettel, Vbee...)
    private String text;
    private String provider; // "FPT", "CAMBAI", "AZURE"
}
