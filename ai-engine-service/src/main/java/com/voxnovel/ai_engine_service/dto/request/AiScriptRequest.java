package com.voxnovel.ai_engine_service.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiScriptRequest {
    private String chapterId;
    private String bookId;
    private String chapterText;            // Chứa toàn bộ nội dung thô cần phân vai
    private List<CharacterInfo> characters; // Danh sách nhân vật có trong chương này
}
