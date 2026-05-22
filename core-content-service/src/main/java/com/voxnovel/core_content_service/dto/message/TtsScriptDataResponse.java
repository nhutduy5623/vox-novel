package com.voxnovel.core_content_service.dto.message;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TtsScriptDataResponse {
    private String novelId;
    private String chapterId;
    private List<TtsScriptLine> scriptLines;

    @Data
    @Builder
    public static class TtsScriptLine {
        private int sequence;
        private String characterId;
        private String voiceId;
        private String provider;
        private String text;
    }
}