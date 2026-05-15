package com.voxnovel.core_content_service.mapper;

import com.voxnovel.core_content_service.dto.request.CreateVoiceRequest;
import com.voxnovel.core_content_service.dto.response.VoiceResponse;
import com.voxnovel.core_content_service.entity.Voice;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface VoiceMapper {

    @Mapping(target = "provider", ignore = true)
    Voice toEntity(CreateVoiceRequest request);

    @Mapping(source = "provider.id", target = "providerId")
    @Mapping(source = "provider.code", target = "providerCode")
    VoiceResponse toResponse(Voice entity);

}