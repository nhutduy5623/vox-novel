package com.voxnovel.core_content_service.dto.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
public class AiScriptResponse {
    private String chapterId;
    private String chapterText;
    private List<CharacterInfo> characters;
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CharacterInfo {
        private String characterId;
        private String name;
        private String gender;
        private String voiceTone; // Map từ cột description của DB sang
        private boolean isNarrator;
    }
}
