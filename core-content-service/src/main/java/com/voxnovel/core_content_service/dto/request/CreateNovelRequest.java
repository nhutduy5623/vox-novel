package com.voxnovel.core_content_service.dto.request;

import com.voxnovel.core_content_service.enums.NovelStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;

@Data
public class CreateNovelRequest {
    @NotBlank(message = "Tên truyện không được để trống")
    private String title;

    private String description;

    private String coverImageUrl;

    @NotNull(message = "Trạng thái không được để trống")
    private NovelStatus status;
}