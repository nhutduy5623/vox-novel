package com.voxnovel.media_tts_service.rabbitmq.listener;

import com.voxnovel.media_tts_service.rabbitmq.TtsProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TtsRequestListener {

    private final TtsProcessingService ttsProcessingService;

    // Lắng nghe ở Queue ĐI của Core Content gửi sang
    @RabbitListener(queues = "${rabbitmq.media.queue.tts-request}")
    public void handleTtsRequest(Long chapterId) {
        log.info("📩 Nhận lệnh sinh Audio từ RabbitMQ cho Chapter ID: {}", chapterId);
        ttsProcessingService.processTtsGeneration(chapterId);
    }
}