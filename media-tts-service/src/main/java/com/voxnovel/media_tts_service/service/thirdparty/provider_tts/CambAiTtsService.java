package com.voxnovel.media_tts_service.service.thirdparty.provider_tts;

import com.voxnovel.media_tts_service.dto.thirdparty.CambAiRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
public class CambAiTtsService implements TtsProviderStrategy {

    // Camb AI URL cố định theo tài liệu
    private static final String CAMBAI_API_URL = "https://client.camb.ai/apis/tts-stream";

    // Sử dụng RestClient hiện đại của Spring Boot 3.2+
    private final RestClient restClient;

    public CambAiTtsService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    @Override
    public String getProviderCode() {
        return "CAMBAI";
    }

    @Override
    public byte[] generateAudio(String text, String voiceIdStr, String apiKey) {
        log.info("Đang gọi CambAI TTS | VoiceID: {} | Text length: {}", voiceIdStr, text.length());

        // 1. Ép kiểu voiceId từ String sang Integer một cách an toàn
        int voiceId;
        try {
            voiceId = Integer.parseInt(voiceIdStr.trim());
        } catch (NumberFormatException e) {
            log.error("CambAI yêu cầu voiceId là số nguyên, nhưng nhận được: {}", voiceIdStr);
            throw new IllegalArgumentException("VoiceID của CambAI không hợp lệ: " + voiceIdStr);
        }

        // 2. Lắp ráp Payload chuẩn OpenAPI của hãng
        CambAiRequest payload = CambAiRequest.builder()
                .text(text)
                .language("vi-vn")
                .voiceId(voiceId)
                .speechModel("mars-8.1-flash-beta") // Model có tốc độ cao và hỗ trợ mp3
                .enhanceNamedEntitiesPronunciation(false)
                .outputConfiguration(new CambAiRequest.OutputConfig("mp3"))
                .voiceSettings(CambAiRequest.VoiceSettings.builder()
                        .enhanceReferenceAudioQuality(false)
                        .maintainSourceAccent(false)
                        .speakingRate(1.2)
                        .build())
                .build();

        try {
            // 3. Bắn Request và hứng cục Byte lẫn Header
            ResponseEntity<byte[]> response = restClient.post()
                    .uri(CAMBAI_API_URL)
                    .header("x-api-key", apiKey) // Chèn API Key được Redis phân bổ
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toEntity(byte[].class); // Hứng toàn bộ Entity để đọc Header

            // 4. Bóc tách Header để kiểm soát chi phí (Rất quan trọng với dân làm API)
            String creditsUsed = response.getHeaders().getFirst("X-Credits-Required");
            log.info("✅ CambAI sinh audio thành công! Chi phí: {} credits.", creditsUsed);

            // Trả về mảng byte (file mp3) để AudioDraftService đem đi upload MinIO
            return response.getBody();

        } catch (Exception e) {
            log.error("❌ Thất bại khi gọi API CambAI: {}", e.getMessage());
            throw new RuntimeException("Lỗi CambAI TTS Provider: " + e.getMessage(), e);
        }
    }
}