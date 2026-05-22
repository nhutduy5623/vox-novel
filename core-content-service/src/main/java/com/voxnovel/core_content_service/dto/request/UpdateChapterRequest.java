package com.voxnovel.core_content_service.dto.request;

import com.voxnovel.core_content_service.enums.ChapterStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateChapterRequest {
    @NotNull(message = "Số thứ tự chương không được để trống")
    private Integer chapterNumber;

    @NotBlank(message = "Tên chương không được để trống")
    private String title;

    @NotBlank(message = "Nội dung gốc không được để trống")
    private String originalContent;

    private ChapterStatus status;
}
