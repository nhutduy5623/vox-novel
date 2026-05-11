package com.voxnovel.ai_engine_service.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiKeyPoolService {
    private final StringRedisTemplate redisTemplate;

    // Tên của chiếc "hộp" chứa API Keys trong Redis
    private static final String API_KEY_POOL = "ai_engine:api_keys_pool";

    @Value("${vox-novel.ai-engine-service.keys}")
    private List<String> apiKeys;


    // KỊCH BẢN LUA CỐT LÕI:
    // 1. Dùng ZRANGE lấy ra 1 Key có điểm (score) thấp nhất (rảnh nhất)
    // 2. Nếu có key, lập tức dùng ZINCRBY cộng thêm 1 điểm vào Key đó
    // 3. Trả về Key đó cho Java. Nếu không có trả về nil.
    // Tất cả diễn ra trong 1 transaction kín của Redis, không ai chen ngang được!
    private static final String GET_KEY_SCRIPT =
            "local key = redis.call('ZRANGE', KEYS[1], 0, 0)[1] " +
                    "if key then " +
                    "  redis.call('ZINCRBY', KEYS[1], 1, key) " +
                    "  return key " +
                    "else " +
                    "  return nil " +
                    "end";



    /**
     * Mẹo nhỏ: Chạy hàm này tự động khi app khởi động để nạp sẵn vài API Key test vào Redis.
     * Sau này có Admin Dashboard thì bạn xóa hàm này đi, cho Admin tự thêm qua giao diện.
     */
    @PostConstruct
    public void initApiKeyPool() {
        log.info("Đang khởi tạo Pool API Key với {} keys...", apiKeys.size());

        for (String key : apiKeys) {
            // Chỉ thêm nếu key chưa tồn tại trong ZSET để tránh ghi đè điểm (score)
            // Điểm mặc định = 0 (Key đang rảnh rỗi tuyệt đối)
            redisTemplate.opsForZSet().addIfAbsent(API_KEY_POOL, key.trim(), 0);
        }
        log.info("Hệ thống đã sẵn sàng với chiến thuật Load Balancer!");
    }

    /**
     * Hàm gọi ra để "xin" Key rảnh nhất
     */
    public String getOptimalApiKey() {
        RedisScript<String> script = new DefaultRedisScript<>(GET_KEY_SCRIPT, String.class);
        String apiKey = redisTemplate.execute(script, Collections.singletonList(API_KEY_POOL));

        if (apiKey == null) {
            throw new RuntimeException("Tất cả API Keys đều đang sập hoặc pool rỗng!");
        }

        log.info("Đã cấp phát API Key: [{}] - Đã cộng 1 điểm bận rộn.", maskApiKey(apiKey));
        return apiKey;
    }

    /**
     * Hàm gọi ra để trả Key về khi AI sinh kịch bản xong
     */
    public void releaseApiKey(String apiKey) {
        redisTemplate.opsForZSet().incrementScore(API_KEY_POOL, apiKey, -1);
        log.info("Đã trả lại API Key: [{}] - Đã trừ 1 điểm bận rộn.", maskApiKey(apiKey));
    }

    // Hàm che giấu Key khi in ra log để bảo mật
    private String maskApiKey(String key) {
        if (key == null || key.length() <= 8) return "***";
        return key.substring(0, 4) + "..." + key.substring(key.length() - 4);
    }
}
