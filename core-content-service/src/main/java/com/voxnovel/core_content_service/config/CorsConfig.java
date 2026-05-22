package com.voxnovel.core_content_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS cho FE dev (Angular). Chỉ áp dụng public API {@code /api/**}.
 * <p>
 * Hiện tại: không cookie / không Auth. Sau này FE gửi JWT qua header {@code Authorization}
 * (thường qua API Gateway) — header đã được allow sẵn; khi dùng cookie + credentials
 * cần bật {@code allowCredentials(true)} và giữ origin cụ thể (không dùng {@code *}).
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private static final String[] ALLOWED_ORIGINS = {
            "http://localhost:4200"
    };

    private static final String[] ALLOWED_METHODS = {
            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
    };

    private static final String[] ALLOWED_HEADERS = {
            "Content-Type",
            "Accept",
            "Authorization"
    };

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(ALLOWED_ORIGINS)
                .allowedMethods(ALLOWED_METHODS)
                .allowedHeaders(ALLOWED_HEADERS)
                .allowCredentials(false)
                .maxAge(3600);
    }
}
