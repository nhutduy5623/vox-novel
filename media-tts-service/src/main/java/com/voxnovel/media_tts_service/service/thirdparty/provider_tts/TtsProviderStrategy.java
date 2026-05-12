package com.voxnovel.media_tts_service.service.thirdparty.provider_tts;

import com.voxnovel.media_tts_service.service.ApiKeyPoolService;

public interface TtsProviderStrategy {
    String getProviderCode();
    byte[] generateAudio(String text, String voiceId, String apiKey);
    // Default method tự động sinh tên Pool, các class con KHÔNG CẦN viết lại hàm này nữa!
    default String getRedisPoolName() {
        return ApiKeyPoolService.POOL_PREFIX + getProviderCode().toLowerCase();
    }
}
