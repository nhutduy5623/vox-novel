package com.voxnovel.media_tts_service.rabbitmq.listener;

import com.voxnovel.media_tts_service.dto.message.MergeAudioRequestMessage;
import com.voxnovel.media_tts_service.rabbitmq.MergeAudioProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MergeAudioRequestListener {

    private final MergeAudioProcessingService mergeAudioProcessingService;

    @RabbitListener(queues = "${rabbitmq.media.queue.merge-request}")
    public void handleMergeRequest(MergeAudioRequestMessage message) {
        log.info("📩 Nhận lệnh Merge Audio | novelId={}, chapterId={}", message.getNovelId(), message.getChapterId());
        mergeAudioProcessingService.processMergeRequest(message);
    }
}

