package com.voxnovel.core_content_service.mapper;

import com.voxnovel.core_content_service.dto.request.CreateChapterRequest;
import com.voxnovel.core_content_service.dto.response.ChapterResponse;
import com.voxnovel.core_content_service.entity.Chapter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ChapterMapper {

    @Mapping(target = "novel", ignore = true)
    @Mapping(target = "status", constant = "DRAFT") // Mặc định khi tạo mới là DRAFT
    Chapter toEntity(CreateChapterRequest request);

    @Mapping(source = "novel.id", target = "novelId")
    @Mapping(target = "characters", ignore = true)
    ChapterResponse toResponse(Chapter entity);
}
