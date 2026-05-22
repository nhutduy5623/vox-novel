package com.voxnovel.core_content_service.rabbitmq.listener;

import com.voxnovel.core_content_service.dto.message.MergeAudioResultMessage;
import com.voxnovel.core_content_service.service.ChapterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MergeAudioResultListener {

    private final ChapterService chapterService;

    @RabbitListener(queues = "${rabbitmq.media.queue.merge-response}")
    public void handleMergeResult(MergeAudioResultMessage message) {
        log.info("📩 Nhận kết quả Merge Audio | novelId={}, chapterId={}, status={}",
                message.getNovelId(), message.getChapterId(), message.getStatus());

        if (message.getChapterId() == null) {
            log.warn("Merge Audio message thiếu dữ liệu: {}", message);
            return;
        }

        if ("SUCCESS".equalsIgnoreCase(message.getStatus())) {
            if (message.getAudioUrl() == null) {
                log.warn("Merge Audio SUCCESS nhưng thiếu audioUrl | chapterId={}", message.getChapterId());
                return;
            }
            chapterService.updateAudioStatus(Long.valueOf(message.getChapterId()), message.getAudioUrl());
        } else {
            log.error("Merge Audio thất bại | chapterId={} | error={}", message.getChapterId(), message.getErrorMessage());
        }
    }
}
