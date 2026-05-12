package com.voxnovel.media_tts_service.service;

import com.voxnovel.media_tts_service.config.TtsProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyPoolService {

    private final StringRedisTemplate redisTemplate;
    private final TtsProperties ttsProperties;

    // Đặt chung một tiền tố cho tất cả các Pool
    public static final String POOL_PREFIX = "media_tts:api_keys_pool:";

    // KỊCH BẢN LUA CỐT LÕI (Không thay đổi)
    private static final String GET_KEY_SCRIPT =
            "local key = redis.call('ZRANGE', KEYS[1], 0, 0)[1] " +
                    "if key then " +
                    "  redis.call('ZINCRBY', KEYS[1], 1, key) " +
                    "  return key " +
                    "else " +
                    "  return nil " +
                    "end";

    @PostConstruct
    public void initApiKeyPool() {
        log.info("Đang quét cấu hình để khởi tạo Pool API Key động...");
        // Quét toàn bộ Map trong application.yml
        ttsProperties.getProviders().forEach((providerName, config) -> {
            String poolName = POOL_PREFIX + providerName.toLowerCase();
            List<String> keys = config.getApiKeys();

            if (keys != null && !keys.isEmpty()) {
                initPool(poolName, keys);
                log.info("Đã nạp {} keys cho nhà cung cấp [{}] vào Pool [{}]",
                        keys.size(), providerName, poolName);
            }
        });
        log.info("Hệ thống ZSET Load Balancer đã sẵn sàng!");
    }

    private void initPool(String poolName, List<String> keys) {
        if (keys == null || keys.isEmpty()) return;
        for (String key : keys) {
            redisTemplate.opsForZSet().addIfAbsent(poolName, key.trim(), 0);
        }
    }

    /**
     * Hàm dùng chung để xin Key từ một Pool bất kỳ
     */
    public String getOptimalApiKey(String poolName) {
        RedisScript<String> script = new DefaultRedisScript<>(GET_KEY_SCRIPT, String.class);
        String apiKey = redisTemplate.execute(script, Collections.singletonList(poolName));

        if (apiKey == null) {
            throw new RuntimeException("Tất cả API Keys của pool [" + poolName + "] đều rỗng!");
        }

        log.info("Đã cấp phát Key từ pool [{}]: [{}] - Đã +1 điểm bận rộn.", poolName, maskApiKey(apiKey));
        return apiKey;
    }

    /**
     * Hàm dùng chung để trả Key về Pool
     */
    public void releaseApiKey(String poolName, String apiKey) {
        if (apiKey != null) {
            redisTemplate.opsForZSet().incrementScore(poolName, apiKey, -1);
            log.info("Đã trả lại Key về pool [{}]: [{}] - Đã -1 điểm bận rộn.", poolName, maskApiKey(apiKey));
        }
    }

    private String maskApiKey(String key) {
        if (key == null || key.length() <= 8) return "***";
        return key.substring(0, 4) + "..." + key.substring(key.length() - 4);
    }
}
