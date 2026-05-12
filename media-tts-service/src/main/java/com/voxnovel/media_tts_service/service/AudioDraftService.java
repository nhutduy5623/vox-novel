package com.voxnovel.media_tts_service.service;

import com.voxnovel.media_tts_service.dto.request.AudioDraftRequest;
import com.voxnovel.media_tts_service.dto.request.DraftLine;
import com.voxnovel.media_tts_service.dto.response.AudioDraftResponse;
import com.voxnovel.media_tts_service.dto.response.AudioLineResult;
import com.voxnovel.media_tts_service.service.thirdparty.provider_tts.TtsProviderStrategy;
import com.voxnovel.media_tts_service.service.thirdparty.MinioService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class AudioDraftService {

    private final ApiKeyPoolService apiKeyPoolService;
    private final MinioService minioService;
    private final StringRedisTemplate redisTemplate;
    private final Map<String, TtsProviderStrategy> ttsStrategyMap = new HashMap<>();

    // TẠO HỒ CHỨA LUỒNG (THREAD POOL)
    // Giới hạn 4-5 luồng chạy song song để tránh bị API bên thứ 3 khóa vì spam quá nhanh (Rate Limit)
    private final Map<String, ExecutorService> providerExecutors = new HashMap<>();

    public AudioDraftService(ApiKeyPoolService apiKeyPoolService,
                             MinioService minioService,
                             StringRedisTemplate redisTemplate,
                             List<TtsProviderStrategy> strategies) {
        this.apiKeyPoolService = apiKeyPoolService;
        this.minioService = minioService;
        this.redisTemplate = redisTemplate;

        for (TtsProviderStrategy strategy : strategies) {
            String code = strategy.getProviderCode().toUpperCase();
            ttsStrategyMap.put(code, strategy);

            // Cấp cho mỗi nhà cung cấp 3-5 luồng chạy song song độc lập
            // (Đạo hữu có thể linh hoạt con số này dựa vào số lượng API Key của từng bên)
            providerExecutors.put(code, Executors.newFixedThreadPool(4));
        }
    }

    public AudioDraftResponse processDraftAudio(AudioDraftRequest request) {
        String lockKey = "media_tts:lock:draft:" + request.getChapterId();
        Boolean isLocked = redisTemplate.opsForValue().setIfAbsent(lockKey, "LOCKED", Duration.ofMinutes(5));

        if (Boolean.FALSE.equals(isLocked)) {
            log.warn("Spam phát hiện! Chapter {} đang được tạo Audio.", request.getChapterId());
            throw new RuntimeException("Chapter này đang được xử lý tạo Audio. Vui lòng đợi vài phút!");
        }

        try {
            // ==== PHÁT ĐỘNG ĐA LUỒNG ====
            // Biến 100 câu thoại thành 100 cái CompletableFuture và quăng vào ThreadPool chạy song song
            List<CompletableFuture<AudioLineResult>> futures = request.getScriptLines().stream()
                    .map(line -> {
                        String providerCode = line.getProvider().toUpperCase();

                        // Lấy đúng đội quân (ThreadPool) của nhà cung cấp đó ra để chạy
                        ExecutorService executor = providerExecutors.get(providerCode);

                        if (executor == null) {
                            throw new IllegalArgumentException("Không tìm thấy Executor cho: " + providerCode);
                        }

                        // Quăng việc cho đúng ThreadPool xử lý
                        return CompletableFuture.supplyAsync(() -> processSingleLine(request, line), executor);
                    })
                    .toList();

            // ==== GOM KẾT QUẢ VÀ SẮP XẾP LẠI ====
            // Vì chạy song song nên câu 5 có thể xong trước câu 1. Ta cần .join() đợi tất cả xong
            // Sau đó dùng .sorted() để ép chúng nó xếp hàng lại từ 1 -> 100 chuẩn chỉ.
            List<AudioLineResult> audioLines = futures.stream()
                    .map(CompletableFuture::join)
                    .sorted(Comparator.comparingInt(AudioLineResult::getSequence))
                    .toList();

            AudioDraftResponse response = new AudioDraftResponse();
            response.setNovelId(request.getNovelId());
            response.setChapterId(request.getChapterId());
            response.setAudioLines(audioLines);
            return response;

        } finally {
            redisTemplate.delete(lockKey);
            log.info("Đã mở khóa an toàn cho Chapter: {}", request.getChapterId());
        }
    }

    /**
     * HÀM XỬ LÝ LÕI CHO 1 CÂU THOẠI (Được chạy bởi các Thread con)
     */
    private AudioLineResult processSingleLine(AudioDraftRequest request, DraftLine line) {
        AudioLineResult result = new AudioLineResult();
        result.setSequence(line.getSequence());
        result.setCharacterId(line.getCharacterId());

        String currentPool = null;
        String assignedKey = null;

        try {
            String providerCode = line.getProvider().toUpperCase();
            TtsProviderStrategy ttsStrategy = ttsStrategyMap.get(providerCode);

            if (ttsStrategy == null) {
                throw new IllegalArgumentException("Chưa hỗ trợ: " + providerCode);
            }

            // Xin Key từ Redis (Redis đơn luồng nên 5 thread vào xin cùng lúc vẫn không bao giờ đụng nhau)
            currentPool = ttsStrategy.getRedisPoolName();
            assignedKey = apiKeyPoolService.getOptimalApiKey(currentPool);

            // Gọi API
            byte[] audioData = ttsStrategy.generateAudio(line.getText(), line.getVoiceId(), assignedKey);

            // Upload MinIO
            String objectKey = String.format("drafts/%s/%s/%03d_%s.mp3",
                    request.getNovelId(), request.getChapterId(), line.getSequence(), line.getCharacterId());
            String fileUrl = minioService.uploadAudioBytes(objectKey, audioData);

            result.setAudioUrl(fileUrl);
            result.setStatus("SUCCESS");

        } catch (Exception e) {
            log.error("Lỗi tại sequence {}: {}", line.getSequence(), e.getMessage());
            result.setStatus("FAILED");
        } finally {
            // Trả Key lại cho Redis để thằng Thread khác lấy xài
            if (assignedKey != null && currentPool != null) {
                apiKeyPoolService.releaseApiKey(currentPool, assignedKey);
            }
        }
        return result;
    }
}