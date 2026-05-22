package com.voxnovel.media_tts_service.rabbitmq;

import com.voxnovel.media_tts_service.dto.message.MergeAudioRequestMessage;
import com.voxnovel.media_tts_service.dto.message.MergeAudioResultMessage;
import com.voxnovel.media_tts_service.dto.request.AudioMergeRequest;
import com.voxnovel.media_tts_service.service.AudioMergeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MergeAudioProcessingService {

    private final RabbitTemplate rabbitTemplate;
    private final AudioMergeService audioMergeService;

    @Value("${rabbitmq.media.exchange}")
    private String exchange;

    @Value("${rabbitmq.media.routing.merge-response}")
    private String mergeResponseRouting;

    public void processMergeRequest(MergeAudioRequestMessage message) {
        try {
            String audioUrl = audioMergeService.processMerge(new AudioMergeRequest(message.getNovelId(), message.getChapterId()));
            rabbitTemplate.convertAndSend(
                    exchange,
                    mergeResponseRouting,
                    new MergeAudioResultMessage(message.getNovelId(), message.getChapterId(), audioUrl, "SUCCESS", null)
            );
        } catch (Exception e) {
            rabbitTemplate.convertAndSend(
                    exchange,
                    mergeResponseRouting,
                    new MergeAudioResultMessage(message.getNovelId(), message.getChapterId(), null, "FAILED", e.getMessage())
            );
        }
    }
}

