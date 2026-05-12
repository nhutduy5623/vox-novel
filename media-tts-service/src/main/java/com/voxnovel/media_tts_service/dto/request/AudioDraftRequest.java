package com.voxnovel.media_tts_service.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class AudioDraftRequest {
    private String novelId;
    private String chapterId;
    private List<DraftLine> scriptLines;
}
