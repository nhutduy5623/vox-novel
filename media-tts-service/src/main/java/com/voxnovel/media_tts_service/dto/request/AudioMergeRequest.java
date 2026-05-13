package com.voxnovel.media_tts_service.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AudioMergeRequest(
        @NotBlank(message = "novelId không được để trống")
        String novelId,

        @NotBlank(message = "chapterId không được để trống")
        String chapterId
) {}