package com.voxnovel.ai_engine_service.service;

import com.voxnovel.ai_engine_service.dto.request.AiScriptRequest;
import com.voxnovel.ai_engine_service.dto.response.AiScriptResponse;
import com.voxnovel.ai_engine_service.dto.response.ScriptLine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiOrchestrationService {

    private final LlmGenerationService llmService;
    private static final int MAX_CHUNK_SIZE = 2000;

    public AiScriptResponse processSync(AiScriptRequest request) {
        log.info("Bắt đầu xử lý song song nội dung dài {} ký tự.", request.getChapterText().length());

        List<String> chunks = splitText(request.getChapterText());

        // Danh sách chứa các "Nhiệm vụ AI" chạy ngầm
        List<CompletableFuture<List<ScriptLine>>> futures = new ArrayList<>();

        String previousContext = "";

        // 1. Chuẩn bị và bắn TẤT CẢ các request cùng một lúc
        for (int i = 0; i < chunks.size(); i++) {
            final String currentChunk = chunks.get(i);
            final String currentContext = previousContext;
            final int chunkIndex = i + 1;

            log.info("Bắn request cho Chunk {}/{} lên Google...", chunkIndex, chunks.size());

            // Đóng gói việc gọi AI vào một luồng (thread) chạy song song
            CompletableFuture<List<ScriptLine>> future = CompletableFuture.supplyAsync(() -> {
                return llmService.generateChunkScript(currentChunk, currentContext, request.getCharacters());
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
                .novelId(request.getNovelId())
                .chapterId(request.getChapterId())
                .status("SUCCESS")
                .scriptLines(allLines)
                .build();
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