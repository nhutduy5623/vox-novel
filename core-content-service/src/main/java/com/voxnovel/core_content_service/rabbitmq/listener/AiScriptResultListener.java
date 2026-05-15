package com.voxnovel.core_content_service.rabbitmq.listener;

import com.voxnovel.core_content_service.dto.message.AiScriptResultMessage;
import com.voxnovel.core_content_service.entity.Chapter;
import com.voxnovel.core_content_service.enums.ChapterStatus;
import com.voxnovel.core_content_service.repository.ChapterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiScriptResultListener {

    private final ChapterRepository chapterRepository;

    @RabbitListener(queues = "ai.script.result.queue")
    @Transactional
    public void handleScriptResult(AiScriptResultMessage message) {
        try {
            log.info("Core Content nhận được kịch bản từ AI Engine: {}", message.getResult().getChapterId());
            AiScriptResultMessage.Result result = message.getResult();
            Long chapterId = Long.valueOf(result.getChapterId());

            // 2. Tìm Chapter trong Database
            Chapter chapter = chapterRepository.findById(chapterId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy Chapter ID: " + chapterId));

            // 3. Xử lý lưu trữ
            if ("SUCCESS".equals(result.getStatus())) {
                // Nhét thẳng List<ScriptLineDto> vào entity, Hibernate 6 sẽ tự động parse ra JSON rắc vào PostgreSQL
                chapter.setScriptData(result.getScriptLines());
                // Đổi trạng thái sang REVIEWING để Client biết vào xem và sửa lỗi
                chapter.setStatus(ChapterStatus.REVIEWING);
                log.info("✅ Lưu kịch bản thành công cho Chapter {}", chapterId);
            } else {
                chapter.setStatus(ChapterStatus.FAILED);
                log.error("❌ AI Engine báo lỗi (FAILED) khi gen Chapter {}", chapterId);
            }

            // 4. Lưu lại vào DB
            chapterRepository.save(chapter);
            // TÙY CHỌN: Ở đây có thể bắn WebSocket về cho Frontend báo "Chương 341 đã phân vai xong!"

        } catch (Exception e) {
            log.error("Lỗi khi xử lý message từ AI Engine: ", e);
            throw new RuntimeException("Lỗi xử lý kết quả AI", e);
        }
    }
}