package com.voxnovel.media_tts_service.controller;

import com.voxnovel.media_tts_service.dto.request.AudioDraftRequest;
import com.voxnovel.media_tts_service.dto.response.ApiResponse;
import com.voxnovel.media_tts_service.dto.response.AudioDraftResponse;
import com.voxnovel.media_tts_service.service.AudioDraftService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
public class MediaController {

    private final AudioDraftService audioDraftService;

    @PostMapping("/generate-draft")
    public ResponseEntity<ApiResponse<AudioDraftResponse>> generateDraftAudio(@RequestBody AudioDraftRequest request) {

        log.info("Nhận API tạo Audio Nháp | Chapter: {} | Số lượng câu thoại: {}",
                request.getChapterId(), request.getScriptLines().size());
        // Xử lý logic
        AudioDraftResponse draftData = audioDraftService.processDraftAudio(request);
        ApiResponse<AudioDraftResponse> response = ApiResponse.<AudioDraftResponse>builder()
                .message("Tạo audio nháp thành công")
                .result(draftData)
                .build();

        return ResponseEntity.ok(response);
    }
}