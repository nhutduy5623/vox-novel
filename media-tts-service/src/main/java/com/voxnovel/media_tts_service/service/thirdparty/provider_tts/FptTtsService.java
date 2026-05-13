package com.voxnovel.media_tts_service.service.thirdparty.provider_tts;

import com.voxnovel.media_tts_service.dto.thirdparty.FptTtsResponse;
import com.voxnovel.media_tts_service.service.ApiKeyPoolService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Service
public class FptTtsService implements TtsProviderStrategy {
    private static final String FPT_API_URL = "https://api.fpt.ai/hmi/tts/v5";
    private final RestClient restClient;
    private final ApiKeyPoolService apiKeyPoolService;

    public FptTtsService(RestClient.Builder restClientBuilder, ApiKeyPoolService apiKeyPoolService) {
        this.restClient = restClientBuilder.build();
        this.apiKeyPoolService = apiKeyPoolService;
    }

    @Override
    public String getProviderCode() {
        return "FPT";
    }

    @Override
    public byte[] generateAudio(String text, String voiceId, String apiKey) {
        log.info("Đang gọi FPT AI TTS | VoiceID: {} | Text length: {}", voiceId, text.length());
        String poolName = ApiKeyPoolService.POOL_PREFIX + getProviderCode().toLowerCase();

        try {
            FptTtsResponse fptResponse = restClient.post()
                    .uri(FPT_API_URL)
                    .header("api_key", apiKey)
                    .header("voice", voiceId.trim())
                    .header("speed", "1") // Đã nâng tốc độ như yêu cầu
                    .header("format", "mp3")
                    .contentType(MediaType.valueOf("text/plain; charset=utf-8"))
                    .body(text.getBytes(java.nio.charset.StandardCharsets.UTF_8))
                    .retrieve()
                    .body(FptTtsResponse.class);

            if (fptResponse == null || fptResponse.error() != 0) {
                String errorMsg = fptResponse != null ? fptResponse.message() : "Unknown";

                // Logic phạt thẻ khi FPT nhả mã lỗi bên trong JSON Body
                if (errorMsg.toLowerCase().contains("limit") || errorMsg.toLowerCase().contains("quota")) {
                    apiKeyPoolService.reportYellowCard(poolName, apiKey);
                } else if (errorMsg.toLowerCase().contains("invalid api_key") || errorMsg.toLowerCase().contains("unauthorized")) {
                    apiKeyPoolService.reportRedCard(poolName, apiKey);
                }

                throw new RuntimeException("FPT AI trả về lỗi: " + errorMsg);
            }

            String asyncUrl = fptResponse.async();
            log.info("FPT đã tiếp nhận. Đang chờ file tại URL: {}", asyncUrl);

            return downloadAudioWithPolling(asyncUrl);

        } catch (HttpClientErrorException.TooManyRequests e) {
            apiKeyPoolService.reportYellowCard(poolName, apiKey);
            throw new RuntimeException("Rate Limit từ FPT: " + e.getMessage(), e);
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            apiKeyPoolService.reportRedCard(poolName, apiKey);
            throw new RuntimeException("API Key FPT không hợp lệ: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("❌ Thất bại khi gọi API FPT: {}", e.getMessage());
            throw new RuntimeException("Lỗi FPT TTS Provider: " + e.getMessage(), e);
        }
    }

    private byte[] downloadAudioWithPolling(String audioUrl) {
        int maxRetries = 30;
        int sleepMillis = 3000;

        for (int i = 1; i <= maxRetries; i++) {
            try {
                ResponseEntity<byte[]> response = restClient.get()
                        .uri(audioUrl)
                        .retrieve()
                        .toEntity(byte[].class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    log.info("✅ Tải thành công file từ FPT sau {} lần thử.", i);
                    return response.getBody();
                }

            } catch (RestClientResponseException e) {
                log.debug("File chưa sẵn sàng (Lần thử {}/{}) - Lỗi: {}", i, maxRetries, e.getStatusCode());
            } catch (Exception e) {
                log.warn("Lỗi mạng khi tải file FPT: {}", e.getMessage());
            }

            try {
                Thread.sleep(sleepMillis);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Tiến trình chờ file FPT bị gián đoạn", ie);
            }
        }
        throw new RuntimeException("Timeout! Không thể tải file từ FPT sau " + (maxRetries * sleepMillis / 1000) + " giây.");
    }
}