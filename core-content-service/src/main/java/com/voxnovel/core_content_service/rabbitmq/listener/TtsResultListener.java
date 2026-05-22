package com.voxnovel.core_content_service.rabbitmq.listener;

import com.voxnovel.core_content_service.dto.message.ScriptLineDto;
import com.voxnovel.core_content_service.dto.message.TtsDraftResultMessage;
import com.voxnovel.core_content_service.entity.Chapter;
import com.voxnovel.core_content_service.enums.ChapterStatus;
import com.voxnovel.core_content_service.repository.ChapterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class TtsResultListener {

    private final ChapterRepository chapterRepository;

    @RabbitListener(queues = "${rabbitmq.media.queue.tts-response}")
    @Transactional
    public void handleTtsResult(TtsDraftResultMessage result) {
        log.info("🎧 Core Content nhận kết quả Audio từ TTS Service cho Chapter: {}", result.getChapterId());

        try {
            Long chapterId = Long.valueOf(result.getChapterId());
            Chapter chapter = chapterRepository.findById(chapterId)
                    .orElseThrow(() -> new RuntimeException("Chapter không tồn tại: " + chapterId));

            // 1. Lấy cục JSON Kịch bản hiện tại ra
            List<ScriptLineDto> scriptData = chapter.getScriptData();

            if (scriptData == null || scriptData.isEmpty()) {
                log.warn("⚠️ Chapter {} không có dữ liệu kịch bản để cập nhật audio!", chapterId);
                return;
            }

            // 2. Tối ưu $O(1)$: Chuyển List kết quả trả về thành Map với Key là `sequence`
            Map<Integer, TtsDraftResultMessage.AudioLineResult> audioResultMap = result.getAudioLines().stream()
                    .collect(Collectors.toMap(
                            TtsDraftResultMessage.AudioLineResult::getSequence,
                            audio -> audio,
                            (existing, replacement) -> existing // Đề phòng lỗi trùng lặp sequence
                    ));

            // 3. Lặp qua kịch bản gốc và ráp Audio Link vào
            boolean isAllSuccess = true; // Biến cờ để check xem có câu nào bị tạch không

            for (ScriptLineDto line : scriptData) {
                TtsDraftResultMessage.AudioLineResult audioResult = audioResultMap.get(line.getSequence());

                if (audioResult != null) {
                    // Ráp data vào DTO
                    // Lưu ý: Vì đạo hữu viết hoa chữ cái đầu (AudioDraftLink, Status)
                    // nên Lombok sẽ sinh ra setter là setAudioDraftLink() và setStatus()
                    line.setAudioDraftLink(audioResult.getAudioUrl());
                    line.setStatus(audioResult.getStatus());

                    // Nếu có 1 câu FAILED thì đánh dấu tổng thể là có lỗi
                    if (!"SUCCESS".equals(audioResult.getStatus())) {
                        isAllSuccess = false;
                    }
                }
            }

            // 4. Nhét ngược lại vào Chapter (Để Hibernate 6 biết JSON này đã bị thay đổi và cần Update)
            chapter.setScriptData(scriptData);

            // 5. Cập nhật trạng thái tổng của Chapter để Frontend biết đường hiển thị
            if (isAllSuccess) {
                chapter.setStatus(ChapterStatus.REVIEWING); // Đạo hữu nhớ thêm enum này nếu chưa có nhé
            } else {
                chapter.setStatus(ChapterStatus.FAILED); // Nếu tạch 1 câu thì cho FAILED để Admin check lại
            }

            // 6. Chốt sổ
            chapterRepository.save(chapter);
            log.info("✅ Cập nhật file Audio thành công vào Database cho Chapter {}", chapterId);

            // TÙY CHỌN: Gọi WebSocket bắn thông báo về cho Admin "Tạo Audio thành công!"

        } catch (Exception e) {
            log.error("❌ Lỗi khi lưu kết quả Audio: ", e);
        }
    }
}