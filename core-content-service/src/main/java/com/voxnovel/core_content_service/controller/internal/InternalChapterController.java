package com.voxnovel.core_content_service.controller.internal;

import com.voxnovel.core_content_service.dto.message.AiScriptResponse;
import com.voxnovel.core_content_service.repository.ChapterRepository;
import com.voxnovel.core_content_service.service.ChapterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/chapters")
@RequiredArgsConstructor
@Slf4j
public class InternalChapterController {
    private final ChapterService chapterService;

    /**
     * API NỘI BỘ: Dành riêng cho AI Engine gọi về để lấy ngữ cảnh (Context)
     * Không public qua API Gateway.
     */
    @GetMapping("/{chapterId}/ai-context")
    public ResponseEntity<AiScriptResponse> getChapterContextForAi(@PathVariable Long chapterId) {
        log.info("Internal API nhận yêu cầu lấy dữ liệu cho AI Engine - Chapter ID: {}", chapterId);
        // Mọi logic tinh hoa (Lọc Narrator, query Cache, bọc JSON) đều đã nằm gọn trong Service
        AiScriptResponse response = chapterService.prepareAiEngineResponse(chapterId);
        return ResponseEntity.ok(response);
    }
}