package com.voxnovel.core_content_service.dto.response;

import com.voxnovel.core_content_service.enums.NovelStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NovelDetailResponse {
    private Long id;
    private String title;
    private String description;
    private String coverImageUrl;
    private NovelStatus status;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<NovelDetailChapterItem> chapters;
    private List<NovelDetailCharacterItem> characters;
}
