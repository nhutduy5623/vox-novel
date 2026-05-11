package com.voxnovel.ai_engine_service.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScriptLine { //1 hàng thoại trong kịch bản JSON
    private int sequence;           // Thứ tự câu thoại (1, 2, 3...)
    private String characterId;     // Khóa ngoại liên kết về characterId gốc
    private String text;            // Nội dung câu thoại
    //private String emotionHint;     // Gợi ý cảm xúc cho hệ thống Text-To-Speech phòng sau này có
}
