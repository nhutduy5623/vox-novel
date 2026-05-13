package com.voxnovel.ai_engine_service.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@EnableScheduling
@Service
@RequiredArgsConstructor
@Slf4j
public class AiKeyPoolService {
    private final StringRedisTemplate redisTemplate;

    // Tên của chiếc "hộp" chứa API Keys trong Redis
    private static final String API_KEY_POOL = "ai_engine:api_keys_pool";

    // Tên của "phòng cách ly" chứa các Key bị Rate Limit 429
    private static final String API_KEY_COOLDOWN = "ai_engine:api_keys_cooldown";

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

    /**
     * KHAI TỬ: Xóa hoàn toàn một Key khỏi hệ thống.
     * Dùng khi Key là đồ giả, bị Google khóa vĩnh viễn, hoặc không khởi tạo được ChatClient.
     */
    public void markKeyAsDead(String apiKey) {
        if (apiKey != null) {
            redisTemplate.opsForZSet().remove(API_KEY_POOL, apiKey);
            log.warn("Đã TRỤC XUẤT vĩnh viễn API Key [{}] khỏi Pool do phát hiện là key ảo hoặc đã tèo!", maskApiKey(apiKey));
        }
    }

    /**
     * PHẠT THẺ VÀNG (Lỗi 429): Chuyển Key sang phòng cách ly trong đúng 60 giây.
     */
    public void reportErrorKey(String apiKey) {
        if (apiKey != null) {
            // 1. Gạch tên khỏi danh sách đang hoạt động
            redisTemplate.opsForZSet().remove(API_KEY_POOL, apiKey);

            // 2. Tính thời gian được thả: Thời điểm hiện tại + 60.000 milliseconds (1 phút)
            long unlockTime = System.currentTimeMillis() + 60000;

            // 3. Tống vào phòng cách ly. (Score chính là lúc được thả)
            redisTemplate.opsForZSet().add(API_KEY_COOLDOWN, apiKey, unlockTime);

            log.warn("Đã PHẠT THẺ VÀNG API Key [{}]. Chuyển vào phòng cách ly 60 giây!", maskApiKey(apiKey));
        }
    }

    /**
     * BÁC SĨ ĐI TUẦN (Chạy ngầm mỗi 10 giây)
     * Kiểm tra phòng cách ly, thả các Key đã mãn hạn tù về lại Pool hoạt động.
     */
    @Scheduled(fixedRate = 10000)
    public void autoHealRateLimitedKeys() {
        long now = System.currentTimeMillis();

        // Query Redis: Lấy ra các Key có Score (Thời gian thả) từ 0 đến Hiện tại (Tức là đã hết hạn 1 phút)
        Set<String> healedKeys = redisTemplate.opsForZSet().rangeByScore(API_KEY_COOLDOWN, 0, now);

        if (healedKeys != null && !healedKeys.isEmpty()) {
            for (String key : healedKeys) {
                // 1. Xóa khỏi phòng cách ly
                redisTemplate.opsForZSet().remove(API_KEY_COOLDOWN, key);

                // 2. Trả lại thẻ xanh, đưa về Pool chính với điểm = 0 (Rảnh rỗi nhất)
                redisTemplate.opsForZSet().add(API_KEY_POOL, key, 0);

                log.info("Đã TỰ ĐỘNG HỒI SINH API Key: [{}] sau khi hết hạn phạt 429", maskApiKey(key));
            }
        }
    }

}
