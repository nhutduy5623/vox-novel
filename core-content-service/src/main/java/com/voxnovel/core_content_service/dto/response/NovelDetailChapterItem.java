package com.voxnovel.core_content_service.dto.response;

import com.voxnovel.core_content_service.enums.ChapterStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NovelDetailChapterItem {
    private Long id;
    private Integer chapterNumber;
    private ChapterStatus status;
}
