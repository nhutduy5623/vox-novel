package com.voxnovel.core_content_service.dto.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScriptLineDto {
    private Integer sequence;
    private String characterId;
    private String text;
    private String audioDraftLink;
    private String status;
}