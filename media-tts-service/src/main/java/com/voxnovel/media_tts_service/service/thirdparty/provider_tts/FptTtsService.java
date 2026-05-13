package com.voxnovel.media_tts_service.service.thirdparty.provider_tts;

import com.voxnovel.media_tts_service.dto.thirdparty.FptTtsResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Service
public class FptTtsService implements TtsProviderStrategy{
    private static final String FPT_API_URL = "https://api.fpt.ai/hmi/tts/v5";
    private final RestClient restClient;

    public FptTtsService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    @Override
    public String getProviderCode() {
        return "FPT";
    }

    @Override
    public byte[] generateAudio(String text, String voiceId, String apiKey) {
        log.info("Đang gọi FPT AI TTS | VoiceID: {} | Text length: {}", voiceId, text.length());

        try {
            // ==== BƯỚC 1: Bắn Text lên để lấy Link Async ====
            FptTtsResponse fptResponse = restClient.post()
                    .uri(FPT_API_URL)
                    .header("api_key", apiKey)
                    .header("voice", voiceId.trim()) // Ví dụ: banmai, leminh
                    .header("speed", "0.5")            // Giữ tốc độ chuẩn
                    .header("format", "mp3")         // Ép định dạng chuẩn mp3
                    .contentType(MediaType.valueOf("text/plain; charset=utf-8")) // Tài liệu ghi truyền Raw data (-d)
                    .body(text.getBytes(java.nio.charset.StandardCharsets.UTF_8))
                    .retrieve()
                    .body(FptTtsResponse.class);

            if (fptResponse == null || fptResponse.error() != 0) {
                String errorMsg = fptResponse != null ? fptResponse.message() : "Unknown";
                throw new RuntimeException("FPT AI trả về lỗi: " + errorMsg);
            }

            String asyncUrl = fptResponse.async();
            log.info("FPT đã tiếp nhận. Đang chờ file tại URL: {}", asyncUrl);

            // ==== BƯỚC 2: POLLING - Đợi và tải file mp3 ====
            return downloadAudioWithPolling(asyncUrl);

        } catch (Exception e) {
            log.error("❌ Thất bại khi gọi API FPT: {}", e.getMessage());
            throw new RuntimeException("Lỗi FPT TTS Provider: " + e.getMessage(), e);
        }
    }

    /**
     * Vòng lặp hỏi thăm FPT cho đến khi file mp3 ra lò
     */
    private byte[] downloadAudioWithPolling(String audioUrl) {
        int maxRetries = 30; // Thử tối đa 30 lần
        int sleepMillis = 3000; // Nghỉ 3 giây mỗi lần (Tổng cộng đợi tối đa 90 giây)

        for (int i = 1; i <= maxRetries; i++) {
            try {
                // Cố gắng tải file về
                ResponseEntity<byte[]> response = restClient.get()
                        .uri(audioUrl)
                        .retrieve()
                        .toEntity(byte[].class);

                // Nếu file đã tồn tại và tải thành công
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    log.info("✅ Tải thành công file từ FPT sau {} lần thử.", i);
                    return response.getBody();
                }

            } catch (RestClientResponseException e) {
                // HTTP 404, 403... nghĩa là FPT chưa gen xong, cứ kệ nó, vòng lặp tiếp theo sẽ hỏi lại
                log.debug("File chưa sẵn sàng (Lần thử {}/{}) - Lỗi: {}", i, maxRetries, e.getStatusCode());
            } catch (Exception e) {
                log.warn("Lỗi mạng khi tải file FPT: {}", e.getMessage());
            }

            // Ngủ một chút chờ hệ thống FPT xử lý (Java 21 Virtual Threads xài Sleep vô tư)
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
