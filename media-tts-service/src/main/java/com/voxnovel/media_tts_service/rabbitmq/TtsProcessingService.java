package com.voxnovel.media_tts_service.rabbitmq;

import com.voxnovel.media_tts_service.client.CoreContentClient;
import com.voxnovel.media_tts_service.dto.request.AudioDraftRequest;
import com.voxnovel.media_tts_service.dto.response.AudioDraftResponse;
import com.voxnovel.media_tts_service.service.AudioDraftService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TtsProcessingService {

    private final CoreContentClient coreContentClient;
    private final RabbitTemplate rabbitTemplate;
    private final AudioDraftService audioDraftService;

    @Value("${rabbitmq.media.exchange}")
    private String exchange;

    @Value("${rabbitmq.media.routing.tts-response}")
    private String responseRoutingKey;

    public void processTtsGeneration(Long chapterId) {
        log.info("🔥 Bắt đầu tiến trình tạo Audio cho Chapter: {}", chapterId);

        try {
            // 1. Dùng Feign gọi ngược về Core lấy Data (Claim Check Pattern)
            AudioDraftRequest scriptData = coreContentClient.getScriptForTts(chapterId);
            log.info("scriptData: ");
            if (scriptData == null || scriptData.getScriptLines() == null || scriptData.getScriptLines().isEmpty()) {
                log.warn("⚠️ Kịch bản trống cho Chapter {}. Hủy xử lý.", chapterId);
                return;
            }
            // 3. Đóng gói kết quả và gửi về Core Content
            AudioDraftResponse finalMessage = audioDraftService.processDraftAudio(scriptData);

            rabbitTemplate.convertAndSend(exchange, responseRoutingKey, finalMessage);
            log.info("✅ Đã gửi báo cáo Audio cho Chapter {} về Core Content.", chapterId);

        } catch (Exception e) {
            log.error("💥 Lỗi hệ thống khi xử lý Chapter {}: ", chapterId, e);
            // Ở đây có thể gửi 1 Message FAILED tổng thể về Core để Admin biết đường xử lý
        }
    }
}