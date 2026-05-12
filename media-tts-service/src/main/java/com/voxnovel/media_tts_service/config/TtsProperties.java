package com.voxnovel.media_tts_service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "vox-novel.tts")
public class TtsProperties {

    // Tự động map các key (fpt, cambai) và list api-keys tương ứng
    private Map<String, ProviderConfig> providers = new HashMap<>();

    @Data
    public static class ProviderConfig {
        private List<String> apiKeys;
    }
}
