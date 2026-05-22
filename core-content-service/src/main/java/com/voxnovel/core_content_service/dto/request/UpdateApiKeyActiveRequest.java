package com.voxnovel.core_content_service.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateApiKeyActiveRequest {
    @NotNull(message = "Trạng thái active không được để trống")
    private Boolean active;
}
