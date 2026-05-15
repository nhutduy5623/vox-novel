package com.voxnovel.core_content_service.dto.request;

import com.voxnovel.core_content_service.enums.ProviderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateProviderRequest {
    @NotBlank(message = "Mã nhà cung cấp không được để trống")
    private String code;

    @NotBlank(message = "Tên nhà cung cấp không được để trống")
    private String name;

    @NotNull(message = "Loại nhà cung cấp không được để trống")
    private ProviderType type;
}