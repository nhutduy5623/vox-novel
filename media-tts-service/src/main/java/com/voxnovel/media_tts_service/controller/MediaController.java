package com.voxnovel.media_tts_service.controller;

import com.voxnovel.media_tts_service.dto.request.AudioDraftRequest;
import com.voxnovel.media_tts_service.dto.request.AudioMergeRequest;
import com.voxnovel.media_tts_service.dto.response.ApiResponse;
import com.voxnovel.media_tts_service.dto.response.AudioDraftResponse;
import com.voxnovel.media_tts_service.service.AudioDraftService;
import com.voxnovel.media_tts_service.service.AudioMergeService;
import com.voxnovel.media_tts_service.service.thirdparty.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    private final AudioMergeService audioMergeService;
    private final MinioService minioService;

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
    @PostMapping("/merge-chapter")
    public ResponseEntity<ApiResponse<String>> mergeAudioChapter(@Validated @RequestBody AudioMergeRequest request) {
        try {
            // Gọi service thực thi đồng bộ để test
            String finalMinioPath = audioMergeService.processMerge(request);
            return ResponseEntity.ok(
                    new ApiResponse<>(200, "Merge thành công!", finalMinioPath)
            );
        } catch (Exception e) {
            // BẮT BUỘC PHẢI CÓ DÒNG NÀY ĐỂ IN LỖI RA CONSOLE
            log.error("Lỗi Exception khi ghép audio: ", e);
            return ResponseEntity.internalServerError().body(
                    new ApiResponse<>(500, "Lỗi ghép audio: " + e.getMessage(), null)
            );
        }
    }

    @GetMapping("/audio/{bucket}/{*path}")
    public ResponseEntity<byte[]> proxyAudio(
            @PathVariable String bucket,
            @PathVariable String path) {
        // Spring Boot 3 sẽ bao gồm cả dấu / ở đầu khi dùng {*path}, cần loại bỏ nó
        String objectKey = (path != null && path.startsWith("/")) ? path.substring(1) : path;
        log.info("Proxy audio request: bucket={}, path={}", bucket, objectKey);

        try {
            byte[] audioBytes = minioService.getFileBytes(objectKey);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("audio/mpeg"));
            headers.setContentLength(audioBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(audioBytes);
        } catch (Exception e) {
            log.error("Lỗi proxy audio: bucket={}, path={}", bucket, objectKey, e);
            return ResponseEntity.notFound().build();
        }
    }
}