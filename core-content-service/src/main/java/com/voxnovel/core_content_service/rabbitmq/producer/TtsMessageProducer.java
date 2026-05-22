package com.voxnovel.core_content_service.rabbitmq.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class TtsMessageProducer {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.media.exchange}")
    private String exchange;

    @Value("${rabbitmq.media.routing.tts-request}")
    private String ttsRequestRouting;

    public void sendTtsGenerateRequest(Long chapterId) {
        log.info("🚀 Core Content yêu cầu TTS Service xử lý Chapter ID: {}", chapterId);
        // Chỉ ném đúng ID sang, phần còn lại kệ TTS tự bơi sang lấy Data
        rabbitTemplate.convertAndSend(exchange, ttsRequestRouting, chapterId);
    }
}