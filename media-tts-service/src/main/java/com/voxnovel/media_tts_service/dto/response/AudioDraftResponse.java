package com.voxnovel.media_tts_service.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class AudioDraftResponse {
    private String novelId;
    private String chapterId;
    private List<AudioLineResult> audioLines;
}
