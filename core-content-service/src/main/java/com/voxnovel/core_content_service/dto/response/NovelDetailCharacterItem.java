package com.voxnovel.core_content_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NovelDetailCharacterItem {
    private Long id;
    private String name;
    private String gender;
    private String previewUrl;
}
