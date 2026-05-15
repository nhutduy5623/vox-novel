package com.voxnovel.core_content_service.mapper;

import com.voxnovel.core_content_service.dto.request.CreateNovelRequest;
import com.voxnovel.core_content_service.dto.response.NovelResponse;
import com.voxnovel.core_content_service.entity.Novel;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

// componentModel = "spring" giúp bạn dùng @RequiredArgsConstructor để Inject thẳng Mapper này vào Service
// unmappedTargetPolicy = ReportingPolicy.IGNORE giúp ẩn cảnh báo khi DTO thiếu một số trường của Entity
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface NovelMapper {

    Novel toEntity(CreateNovelRequest request);

    NovelResponse toResponse(Novel entity);
}