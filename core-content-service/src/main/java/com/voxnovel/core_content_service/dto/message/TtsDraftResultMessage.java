package com.voxnovel.core_content_service.dto.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor // Nhớ thêm NoArgsConstructor cho class con
@AllArgsConstructor
public class TtsDraftResultMessage {
    private String novelId;
    private String chapterId;
    private List<AudioLineResult> audioLines;

    @Data
    @NoArgsConstructor // Nhớ thêm NoArgsConstructor cho class con
    @AllArgsConstructor
    public static class AudioLineResult {
        private int sequence;
        private String characterId;
        private String audioUrl; // Link MinIO
        private String status;   // SUCCESS / FAILED
    }
}

