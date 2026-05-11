package com.voxnovel.ai_engine_service.controller;

import com.voxnovel.ai_engine_service.dto.request.AiScriptRequest;
import com.voxnovel.ai_engine_service.dto.response.AiScriptResponse;
import com.voxnovel.ai_engine_service.dto.response.ApiResponse;
import com.voxnovel.ai_engine_service.service.AiOrchestrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai-engine")
@RequiredArgsConstructor
@Slf4j
public class AiEngineController {
    private final AiOrchestrationService aiOrchestrationService;

    @PostMapping("/generate-script")
    public ResponseEntity<ApiResponse<AiScriptResponse>> generateScript(@RequestBody AiScriptRequest request) {
        log.info("Nhận yêu cầu phân vai cho Chapter ID: {}", request.getChapterId());

        try {
            // Đẩy xuống tầng Service để cắt Chunk và gọi AI
            AiScriptResponse responseData = aiOrchestrationService.processSync(request);

            // Đóng gói thành công vào khuôn
            ApiResponse<AiScriptResponse> response = ApiResponse.<AiScriptResponse>builder()
                    .code(1000)
                    .message("Script generation completed")
                    .result(responseData)
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Lỗi hệ thống khi xử lý AI: ", e);

            // Đóng gói lỗi vào khuôn
            ApiResponse<AiScriptResponse> errorResponse = ApiResponse.<AiScriptResponse>builder()
                    .code(5000)
                    .message("System Error: " + e.getMessage())
                    .result(null)
                    .build();

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
