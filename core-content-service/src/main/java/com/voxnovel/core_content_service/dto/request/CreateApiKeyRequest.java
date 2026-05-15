package com.voxnovel.core_content_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateApiKeyRequest {
    @NotNull(message = "ID nhà cung cấp không được để trống")
    private Long providerId;

    @NotBlank(message = "Chuỗi API Key không được để trống")
    private String keyValue;
}