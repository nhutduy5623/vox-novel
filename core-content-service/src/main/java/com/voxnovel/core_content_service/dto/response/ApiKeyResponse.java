package com.voxnovel.core_content_service.dto.response;

import lombok.Data;

@Data
public class ApiKeyResponse {
    private Long id;
    private Long providerId;
    private String providerName; // Tên hiển thị (VD: OpenAI)
    private String maskedKeyValue; // Chỉ hiển thị dạng: sk-xxxx...1234
    private boolean isActive;
    private String createdBy;
}