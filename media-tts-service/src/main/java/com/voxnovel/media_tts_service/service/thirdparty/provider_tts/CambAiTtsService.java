package com.voxnovel.media_tts_service.service.thirdparty.provider_tts;

import com.voxnovel.media_tts_service.dto.thirdparty.CambAiRequest;
import com.voxnovel.media_tts_service.service.ApiKeyPoolService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
public class CambAiTtsService implements TtsProviderStrategy {

    private static final String CAMBAI_API_URL = "https://client.camb.ai/apis/tts-stream";
    private final RestClient restClient;
    private final ApiKeyPoolService apiKeyPoolService; // Tiêm bộ quản lý thẻ phạt

    public CambAiTtsService(RestClient.Builder restClientBuilder, ApiKeyPoolService apiKeyPoolService) {
        this.restClient = restClientBuilder.build();
        this.apiKeyPoolService = apiKeyPoolService;
    }

    @Override
    public String getProviderCode() {
        return "CAMBAI";
    }

    @Override
    public byte[] generateAudio(String text, String voiceIdStr, String apiKey) {
        log.info("Đang gọi CambAI TTS | VoiceID: {} | Text length: {}", voiceIdStr, text.length());

        int voiceId;
        try {
            voiceId = Integer.parseInt(voiceIdStr.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("VoiceID của CambAI không hợp lệ: " + voiceIdStr);
        }

        CambAiRequest payload = CambAiRequest.builder()
                .text(text)
                .language("vi-vn")
                .voiceId(voiceId)
                .speechModel("mars-8.1-flash-beta")
                .enhanceNamedEntitiesPronunciation(false)
                .outputConfiguration(new CambAiRequest.OutputConfig("mp3"))
                .voiceSettings(CambAiRequest.VoiceSettings.builder()
                        .enhanceReferenceAudioQuality(false)
                        .maintainSourceAccent(false)
                        .speakingRate(1.1)
                        .build())
                .build();

        String poolName = ApiKeyPoolService.POOL_PREFIX + getProviderCode().toLowerCase();

        try {
            ResponseEntity<byte[]> response = restClient.post()
                    .uri(CAMBAI_API_URL)
                    .header("x-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toEntity(byte[].class);

            String creditsUsed = response.getHeaders().getFirst("X-Credits-Required");
            log.info("✅ CambAI sinh audio thành công! Chi phí: {} credits.", creditsUsed);
            return response.getBody();

        } catch (HttpClientErrorException.TooManyRequests e) {
            // Lỗi 429: Phạt thẻ vàng
            apiKeyPoolService.reportYellowCard(poolName, apiKey);
            throw new RuntimeException("Rate Limit từ CambAI: " + e.getMessage(), e);
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            // Lỗi 401, 403: Phạt thẻ đỏ
            apiKeyPoolService.reportRedCard(poolName, apiKey);
            throw new RuntimeException("API Key CambAI không hợp lệ: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("❌ Lỗi kỹ thuật gọi CambAI: {}", e.getMessage());
            throw new RuntimeException("Lỗi CambAI TTS Provider: " + e.getMessage(), e);
        }
    }
}