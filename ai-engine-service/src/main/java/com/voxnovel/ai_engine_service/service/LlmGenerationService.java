package com.voxnovel.ai_engine_service.service;
import com.voxnovel.ai_engine_service.dto.request.CharacterInfo;
import com.voxnovel.ai_engine_service.dto.response.ScriptLine;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import com.google.genai.Client;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class LlmGenerationService {

    private final AiKeyPoolService aiKeyPoolService;

    // Đọc danh sách Key từ file cấu hình (hoặc .env)
    @Value("${vox-novel.ai-engine-service.keys}")
    private List<String> apiKeys;

    // Kho chứa các nòng súng: Mỗi Key đi kèm một ChatClient riêng
    private final Map<String, ChatClient> chatClientPool = new ConcurrentHashMap<>();

    // Hàm này chạy ngay sau khi Spring Boot khởi tạo xong class này
    @PostConstruct
    public void initChatClients() {
        log.info("Đang đúc {} cỗ máy ChatClient độc lập...", apiKeys.size());
        for (String key : apiKeys) {
            String trimmedKey = key.trim();

            // 1. Lắp đạn: Tạo Client bằng SDK chính gốc của Google GenAI
            Client genAiClient = Client.builder().apiKey(trimmedKey).build();

            // 2. Ráp nòng: Nạp cái Client đó vào ChatModel của Spring AI
            GoogleGenAiChatModel chatModel = GoogleGenAiChatModel.builder()
                    .genAiClient(genAiClient)
                    .build();

            // 3. Hoàn thiện súng: Bọc nó lại bằng giao diện ChatClient quen thuộc
            ChatClient client = ChatClient.builder(chatModel).build();

            // Cất vào kho
            chatClientPool.put(trimmedKey, client);
        }
    }

    public List<ScriptLine> generateChunkScript(String textChunk, String previousContext, List<CharacterInfo> characters) {
        int maxRetries = apiKeys.size();
        Exception lastException = null;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            String bestKey = aiKeyPoolService.getOptimalApiKey(); // 1. Gọi Redis xin cái Key rảnh nhất
            if (bestKey == null) {
                throw new RuntimeException("Hệ thống đã cạn kiệt API Key khả dụng!");
            }
            log.info("Đang dùng Key [{}] để xử lý...", maskApiKey(bestKey));
            // 2. Kiểm tra xem Key này có ChatClient tương ứng không
            ChatClient activeClient = chatClientPool.get(bestKey);
            if (activeClient == null) {
                log.warn("Key [{}] là Key ảo hoặc đã bị xóa. Tiến hành cách ly!", maskApiKey(bestKey));
                aiKeyPoolService.markKeyAsDead(bestKey); // Hàm mới cần thêm
                continue; // Bỏ qua, quay lại đầu vòng lặp bốc key khác
            }

            var outputConverter = new BeanOutputConverter<>(new ParameterizedTypeReference<List<ScriptLine>>() {});
            String systemPrompt = """
                Bạn là một chuyên gia biên kịch âm thanh. Nhiệm vụ của bạn là phân vai đoạn truyện thành kịch bản audio.
                
                Danh sách nhân vật hiện có: {character_list}
                
                [NGỮ CẢNH THAM KHẢO]
                Đây là phần cuối của đoạn truyện trước. CHỈ dùng để hiểu bối cảnh và suy luận nhân vật (ví dụ: ai đang nói, 'hắn/y' là ai).
                TUYỆT ĐỐI KHÔNG trích xuất hay phân vai phần này vào kết quả JSON:
                \"\"\"{previous_context}\"\"\"
                
                [NỘI DUNG CẦN PHÂN VAI]
                Chỉ phân tích và trả về kịch bản cho đoạn văn bản dưới đây:
                
                QUY TẮC SỐNG CÒN:
                1. Chỉ lấy text từ phần [NỘI DUNG CẦN PHÂN VAI]. Bỏ qua hoàn toàn text ở phần [NGỮ CẢNH THAM KHẢO].
                2. KHÔNG được tự bịa ra characterId không có trong danh sách.
                3. Toàn bộ miêu tả khung cảnh, nội tâm phải gán cho Người Dẫn Truyện (isNarrator = true).
                4. YÊU CẦU CHIA NHỎ DÒNG: Mỗi câu thoại hoặc tiếng hô của Quần chúng PHẢI nằm trên một dòng riêng biệt với cùng một ID char_mob_04. Tuyệt đối không gộp các câu bàn tán khác nhau vào cùng một trường text.
                {format}
            """;
            try {
                // 3. Dùng activeClient để gọi AI
                List<ScriptLine> result = activeClient.prompt()
                        .system(sp -> sp.text(systemPrompt)
                                .param("character_list", characters.toString())
                                .param("previous_context", previousContext != null ? previousContext : "Không có ngữ cảnh trước.")
                                .param("format", outputConverter.getFormat()))
                        .user(textChunk)
                        .options(GoogleGenAiChatOptions.builder()
                                .model("gemini-3-flash-preview")
                                .temperature(0.3)
                                .build())
                        .call()
                        .entity(outputConverter);
                // 4. THÀNH CÔNG: Trả Key về Redis và kết thúc
                aiKeyPoolService.releaseApiKey(bestKey);
                return result;
            } catch (Exception e) {
                lastException = e;
                log.error("Key [{}] gãy cánh! Lỗi: {}", maskApiKey(bestKey), e.getMessage());
                // 5. THẤT BẠI: Đánh dấu Key này bị lỗi (khóa tạm thời hoặc xóa)
                // KHÔNG gọi releaseApiKey ở đây nữa, vì ta không muốn người khác bốc phải Key rác này
                aiKeyPoolService.reportErrorKey(bestKey);
            }
        }
        // 6. Nếu chạy hết vòng lặp mà vẫn tới đây, tức là mọi Key đều chết
        log.error("Toàn bộ {} API Keys đều đã thất bại!", maxRetries);
        throw new RuntimeException("Lỗi sinh kịch bản AI sau nhiều lần thử: " + lastException.getMessage());
    }

    // Hàm tiện ích che mờ Key khi in log (Security)
    private String maskApiKey(String key) {
        if (key == null || key.length() < 8) return "***";
        return key.substring(0, 4) + "..." + key.substring(key.length() - 4);
    }
}