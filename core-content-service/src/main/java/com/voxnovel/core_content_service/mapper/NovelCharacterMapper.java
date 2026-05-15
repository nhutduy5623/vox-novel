package com.voxnovel.core_content_service.mapper;

import com.voxnovel.core_content_service.dto.request.CreateCharacterRequest;
import com.voxnovel.core_content_service.dto.response.NovelCharacterResponse;
import com.voxnovel.core_content_service.entity.NovelCharacter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface NovelCharacterMapper {

    @Mapping(target = "novel", ignore = true)
    @Mapping(target = "defaultVoice", ignore = true)
    NovelCharacter toEntity(CreateCharacterRequest request);

    @Mapping(source = "novel.id", target = "novelId")
    @Mapping(source = "defaultVoice.id", target = "defaultVoiceId")
    @Mapping(source = "defaultVoice.name", target = "defaultVoiceName")
    NovelCharacterResponse toResponse(NovelCharacter entity);
}