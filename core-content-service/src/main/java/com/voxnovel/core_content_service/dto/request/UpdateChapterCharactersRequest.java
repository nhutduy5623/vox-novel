package com.voxnovel.core_content_service.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class UpdateChapterCharactersRequest {
    @NotNull(message = "Danh sách nhân vật không được null")
    private List<Long> characterIds;
}
