package com.voxnovel.ai_engine_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiScriptResponse {
    private String chapterId;
    // Các trạng thái khác ("PARTIAL_SUCCESS", "RATE_LIMITED"...)
    private String status;
    private List<ScriptLine> scriptLines;
    // Lưu lại thông báo lỗi (nếu có) để Core Content lưu vào log DB
    private String errorMessage;
}
