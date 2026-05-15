package com.voxnovel.ai_engine_service.service;

import com.voxnovel.ai_engine_service.dto.request.AiScriptRequest;
import com.voxnovel.ai_engine_service.dto.response.AiScriptResponse;
import com.voxnovel.ai_engine_service.dto.response.ScriptLine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiOrchestrationService {

    private final LlmGenerationService llmService;
    private static final int MAX_CHUNK_SIZE = 2000;
    private final StringRedisTemplate redisTemplate;

    public AiScriptResponse processSync(AiScriptRequest request) {
        log.info("Bắt đầu xử lý song song nội dung dài {} ký tự.", request.getChapterText().length());
        String chapterId = request.getChapterId();
        String lockKey = "ai:cooldown:chapter:" + chapterId;

        // KIỂM TRA KHÓA CHAPTER (RATE LIMIT 10 PHÚT)
        // setIfAbsent: Nếu key chưa tồn tại -> Tạo key, set thời gian 10 phút, trả về TRUE. Nếu key đã tồn tại -> Trả về FALSE
        Boolean isAllowed = redisTemplate.opsForValue().setIfAbsent(lockKey, "LOCKED", 10, TimeUnit.MINUTES);
        if (Boolean.FALSE.equals(isAllowed)) {
            log.warn("⏳ Lệnh Gen Chapter {} bị chặn do đang trong thời gian Cooldown (10 phút).", chapterId);
            return AiScriptResponse.builder()
                    .chapterId(chapterId)
                    .status("RATE_LIMITED")
                    .errorMessage("Chapter này đang được AI xử lý hoặc vừa xử lý xong. Vui lòng đợi 10 phút trước khi thử lại!")
                    .build();
        }
        try {
            List<String> chunks = splitText(request.getChapterText());
            // Danh sách chứa các "Nhiệm vụ AI" chạy ngầm
            List<CompletableFuture<List<ScriptLine>>> futures = new ArrayList<>();

            String previousContext = "";

            // 1. Chuẩn bị và bắn TẤT CẢ các request cùng một lúc
            for (int i = 0; i < chunks.size(); i++) {
                final String currentChunk = chunks.get(i);
                final String nextChunk = i + 1 < chunks.size() ? chunks.get(i + 1) : "";
                final String currentContext = previousContext;
                final String nextContext = nextChunk.substring(0, Math.min(200, nextChunk.length()));
                final int chunkIndex = i + 1;

                log.info("Bắn request cho Chunk {}/{} lên Google...", chunkIndex, chunks.size());

                // Đóng gói việc gọi AI vào một luồng (thread) chạy song song
                CompletableFuture<List<ScriptLine>> future = CompletableFuture.supplyAsync(() -> {
                    return llmService.generateChunkScript(currentChunk, currentContext, nextContext, request.getCharacters());
                });

                futures.add(future);

                // Tính toán trước Context cho vòng lặp tiếp theo
                previousContext = currentChunk.substring(Math.max(0, currentChunk.length() - 200));
            }

            // 2. Gom kết quả (Đợi tất cả các tiến trình song song chạy xong)
            log.info("Đang chờ AI trả về kết quả đồng loạt...");
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            // 3. Ráp nối và đánh lại số thứ tự (sequence)
            List<ScriptLine> allLines = new ArrayList<>();
            int currentSequence = 1;

            for (CompletableFuture<List<ScriptLine>> future : futures) {
                try {
                    List<ScriptLine> chunkLines = future.get(); // Lấy kết quả từ luồng
                    if (chunkLines != null) {
                        for (ScriptLine line : chunkLines) {
                            line.setSequence(currentSequence++);
                            allLines.add(line);
                        }
                    }
                } catch (Exception e) {
                    log.error("Có lỗi khi gom kết quả từ luồng song song", e);
                }
            }

            log.info("Hoàn tất! Tổng cộng: {} dòng kịch bản.", allLines.size());

            return AiScriptResponse.builder()
                    .chapterId(request.getChapterId())
                    .status("SUCCESS")
                    .scriptLines(allLines)
                    .build();
        } catch (Exception e) {
            // Nếu có lỗi hệ thống văng ra giữa chừng (VD: rớt mạng, lỗi text,...)
            // Ta chủ động xóa Khóa này đi để Admin có thể bấm thử lại NGAY LẬP TỨC mà không phải đợi 10 phút.
            log.error("Lỗi hệ thống khi Gen Chapter {}. Đang nhả khóa Cooldown...", chapterId);
            redisTemplate.delete(lockKey);
            throw e;
        }
    }

    private List<String> splitText(String text) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + MAX_CHUNK_SIZE, text.length());
            if (end < text.length()) {
                int lastNewline = text.lastIndexOf("\n", end);
                if (lastNewline > start) end = lastNewline;
            }
            chunks.add(text.substring(start, end).trim());
            start = end;
        }
        return chunks;
    }
}