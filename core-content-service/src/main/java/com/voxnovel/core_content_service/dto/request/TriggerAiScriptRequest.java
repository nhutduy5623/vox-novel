package com.voxnovel.core_content_service.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
@Data
public class TriggerAiScriptRequest {
    @NotNull(message = "ID chương không được để trống")
    private Long chapterId;

    @NotEmpty(message = "Phải chọn ít nhất 1 nhân vật cho chương này")
    private List<Long> characterIds; // Danh sách ID các nhân vật được Admin tick chọn
}
