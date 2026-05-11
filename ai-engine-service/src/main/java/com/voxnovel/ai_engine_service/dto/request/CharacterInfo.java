package com.voxnovel.ai_engine_service.dto.request;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CharacterInfo {
    private String characterId;
    private String name;
    private String gender;  //Có thể thành Enum
    private String voiceTone;   //Tính cách, sắc giọng
    // Người dẫn truyện
    private boolean isNarrator;
}
