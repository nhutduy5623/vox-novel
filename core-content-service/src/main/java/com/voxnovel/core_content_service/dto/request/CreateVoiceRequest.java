package com.voxnovel.core_content_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateVoiceRequest {
    @NotNull(message = "ID nhà cung cấp không được để trống")
    private Long providerId;

    @NotBlank(message = "Provider Voice ID không được để trống")
    private String providerVoiceId;

    @NotBlank(message = "Tên hiển thị không được để trống")
    private String name;

    private String gender;
    private String language;
    private String previewUrl;
}