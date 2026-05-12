package com.voxnovel.media_tts_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {
    @Bean
    public RestClient.Builder restClientBuilder() {
        // Cấu hình lõi HTTP Client để tăng thời gian chờ
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000); // Thời gian tối đa để kết nối (10 giây)
        factory.setReadTimeout(300000);   // Thời gian tối đa để chờ TTS sinh xong audio (120 giây = 2 phút)

        // Trả về Builder đã được độ chế
        return RestClient.builder().requestFactory(factory);
    }
}
