package com.voxnovel.core_content_service.dto.message;

import lombok.Data;

import java.util.List;

@Data
public class AiScriptResultMessage {
    private String chapterId; // AI trả về kiểu "chap_341"
    private String status;    // "SUCCESS" hoặc "FAILED"
    // Tái sử dụng luôn DTO của Entity Chapter, Spring Jackson sẽ tự động parse!
    private List<ScriptLineDto> scriptLines;
}
