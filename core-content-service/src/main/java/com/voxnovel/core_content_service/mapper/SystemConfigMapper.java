package com.voxnovel.core_content_service.mapper;

import com.voxnovel.core_content_service.dto.request.CreateApiKeyRequest;
import com.voxnovel.core_content_service.dto.request.CreateProviderRequest;
import com.voxnovel.core_content_service.dto.response.ApiKeyResponse;
import com.voxnovel.core_content_service.entity.ApiKey;
import com.voxnovel.core_content_service.entity.Provider;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SystemConfigMapper {

    // --- MAPPING CHO PROVIDER ---
    Provider toProviderEntity(CreateProviderRequest request);

    // --- MAPPING CHO API KEY ---
    @Mapping(target = "provider", ignore = true)
    ApiKey toApiKeyEntity(CreateApiKeyRequest request);

    // Map các thông tin từ Provider (Entity cha) sang DTO
    @Mapping(source = "provider.id", target = "providerId")
    @Mapping(source = "provider.name", target = "providerName")
    // Dùng expression để gọi hàm che chuỗi key ở bên dưới
    @Mapping(target = "maskedKeyValue", expression = "java(maskApiKey(entity.getKeyValue()))")
    ApiKeyResponse toApiKeyResponse(ApiKey entity);

    // Logic che giấu API Key (chỉ giữ lại 4 ký tự cuối)
    @Named("maskApiKey")
    default String maskApiKey(String fullKey) {
        if (fullKey == null || fullKey.length() <= 8) {
            return "********";
        }
        return "sk-****..." + fullKey.substring(fullKey.length() - 4);
    }
}