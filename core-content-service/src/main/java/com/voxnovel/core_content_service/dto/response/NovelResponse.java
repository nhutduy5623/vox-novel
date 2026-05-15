package com.voxnovel.core_content_service.dto.response;

import com.voxnovel.core_content_service.enums.NovelStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NovelResponse {
    private Long id;
    private String title;
    private String description;
    private String coverImageUrl;
    private NovelStatus status;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}