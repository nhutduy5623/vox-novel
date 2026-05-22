package com.voxnovel.core_content_service.rabbitmq.producer;

import com.voxnovel.core_content_service.dto.message.MergeAudioRequestMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MergeAudioProducer {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.media.exchange}")
    private String exchange;

    @Value("${rabbitmq.media.routing.merge-request}")
    private String mergeRequestRouting;

    public void sendMergeChapterRequest(MergeAudioRequestMessage message) {
        log.info("🚀 Core Content gửi lệnh Merge Audio | novelId={}, chapterId={}", message.getNovelId(), message.getChapterId());
        rabbitTemplate.convertAndSend(exchange, mergeRequestRouting, message);
    }
}

