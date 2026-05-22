package com.voxnovel.media_tts_service.dto.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MergeAudioRequestMessage {
    private String novelId;
    private String chapterId;
}

