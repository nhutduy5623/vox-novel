package com.voxnovel.media_tts_service.dto.thirdparty;

import lombok.Builder;

@Builder
public record FptTtsResponse(
        int error,
        String async,
        String message,
        String request_id
) {}
