package com.voxnovel.core_content_service.dto.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MergeAudioResultMessage {
    private String novelId;
    private String chapterId;
    private String audioUrl;
    private String status;
    private String errorMessage;
}

