package com.voxnovel.ai_engine_service.rabbitmq.listener;

import com.voxnovel.ai_engine_service.client.CoreContentClient;
import com.voxnovel.ai_engine_service.config.RabbitMQConfig;
import com.voxnovel.ai_engine_service.dto.request.AiScriptRequest;
import com.voxnovel.ai_engine_service.dto.response.AiScriptResponse;
import com.voxnovel.ai_engine_service.dto.response.ScriptLine;
import com.voxnovel.ai_engine_service.service.AiKeyPoolService;
import com.voxnovel.ai_engine_service.service.AiOrchestrationService;
import com.voxnovel.ai_engine_service.service.LlmGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiScriptGenerateListener {

    private final CoreContentClient coreContentClient;
    private final AiOrchestrationService aiOrchestrationService;
    private final RabbitTemplate rabbitTemplate;

    // LẮNG NGHE Ở HÒM THƯ REQUEST - HỨNG THẲNG KIỂU LONG
    @RabbitListener(queues = RabbitMQConfig.REQUEST_QUEUE)
    public void handleGenerateRequest(Long chapterId) {
        // Không cần bóc tách từ DTO nữa, xài luôn chapterId truyền vào!
        log.info("📥 [AI Engine] Nhận lệnh phân vai cho Chapter ID: {}", chapterId);

        String apiKey = null;
        try {
            // Bước 1: Lấy data từ Core
            AiScriptRequest contextData = coreContentClient.getChapterContext(chapterId);
            if (contextData == null ) {
                throw new RuntimeException("Core Content trả về dữ liệu rỗng!");
            }
            // Bước 3: Gọi AI
            AiScriptResponse response = aiOrchestrationService.processSync(contextData);

            // Bước 4: Trả kết quả
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.AI_EXCHANGE,
                    RabbitMQConfig.RESPONSE_ROUTING_KEY,
                    response
            );
            log.info("✅ [AI Engine] Đã gen xong và gửi kịch bản Chapter {} về cho Core Content", chapterId);
        } catch (Exception e) {
            log.error("❌ [AI Engine] Lỗi nghiêm trọng khi xử lý Chapter {}: ", chapterId, e);
            // Xử lý báo lỗi cực nhanh với chính class này
            AiScriptResponse errorResponse = new AiScriptResponse();
            errorResponse.setChapterId(chapterId.toString());
            errorResponse.setStatus("FAILED");
            errorResponse.setErrorMessage(e.getMessage()); // Truyền lỗi về cho Core lưu Log DB
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.AI_EXCHANGE,
                    RabbitMQConfig.RESPONSE_ROUTING_KEY,
                    errorResponse
            );
        }
    }
}
