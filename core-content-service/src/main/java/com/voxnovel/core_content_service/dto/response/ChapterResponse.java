package com.voxnovel.core_content_service.dto.response;

import com.voxnovel.core_content_service.dto.message.ScriptLineDto;
import com.voxnovel.core_content_service.enums.ChapterStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ChapterResponse {
    private Long id;
    private Long novelId; // Chỉ cần trả về ID của truyện để tối ưu payload
    private Integer chapterNumber;
    private String title;
    private String originalContent;
    private List<ScriptLineDto> scriptData; // Danh sách kịch bản JSON
    private String audioUrl;
    private Boolean hasAudio;
    private ChapterStatus status;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
