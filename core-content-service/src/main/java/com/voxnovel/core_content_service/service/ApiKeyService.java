package com.voxnovel.core_content_service.service;

import com.voxnovel.core_content_service.dto.request.CreateApiKeyRequest;
import com.voxnovel.core_content_service.dto.request.UpdateApiKeyActiveRequest;
import com.voxnovel.core_content_service.dto.response.ApiKeyResponse;
import com.voxnovel.core_content_service.entity.ApiKey;
import com.voxnovel.core_content_service.entity.Provider;
import com.voxnovel.core_content_service.exception.BadRequestException;
import com.voxnovel.core_content_service.exception.ResourceNotFoundException;
import com.voxnovel.core_content_service.mapper.SystemConfigMapper;
import com.voxnovel.core_content_service.repository.ApiKeyRepository;
import com.voxnovel.core_content_service.repository.ProviderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final ProviderRepository providerRepository;
    private final SystemConfigMapper systemConfigMapper;

    @Transactional(readOnly = true)
    public List<ApiKeyResponse> getApiKeys(Long providerId) {
        if (providerId != null) {
            assertProviderExists(providerId);
            return apiKeyRepository.findByProvider_IdOrderByIdDesc(providerId).stream()
                    .map(systemConfigMapper::toApiKeyResponse)
                    .toList();
        }
        return apiKeyRepository.findAllByOrderByIdDesc().stream()
                .map(systemConfigMapper::toApiKeyResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ApiKeyResponse getById(Long id) {
        return systemConfigMapper.toApiKeyResponse(findApiKeyOrThrow(id));
    }

    @Transactional
    public ApiKeyResponse createApiKey(String userId, CreateApiKeyRequest request) {
        Provider provider = providerRepository.findById(request.getProviderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy Provider với ID: " + request.getProviderId()));

        if (apiKeyRepository.existsByKeyValue(request.getKeyValue())) {
            throw new BadRequestException("API Key đã tồn tại trong hệ thống");
        }

        ApiKey apiKey = systemConfigMapper.toApiKeyEntity(request);
        apiKey.setProvider(provider);
        apiKey.setCreatedBy(userId);
        apiKey.setActive(true);

        return systemConfigMapper.toApiKeyResponse(apiKeyRepository.save(apiKey));
    }

    @Transactional
    public ApiKeyResponse updateApiKey(Long id, CreateApiKeyRequest request) {
        ApiKey apiKey = findApiKeyOrThrow(id);

        Provider provider = providerRepository.findById(request.getProviderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy Provider với ID: " + request.getProviderId()));

        apiKeyRepository.findByKeyValue(request.getKeyValue())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BadRequestException("API Key đã tồn tại trong hệ thống");
                });

        apiKey.setProvider(provider);
        apiKey.setKeyValue(request.getKeyValue());

        return systemConfigMapper.toApiKeyResponse(apiKeyRepository.save(apiKey));
    }

    @Transactional
    public ApiKeyResponse updateActiveStatus(Long id, UpdateApiKeyActiveRequest request) {
        ApiKey apiKey = findApiKeyOrThrow(id);
        apiKey.setActive(request.getActive());
        return systemConfigMapper.toApiKeyResponse(apiKeyRepository.save(apiKey));
    }

    @Transactional
    public void deleteApiKey(Long id) {
        if (!apiKeyRepository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy API Key với ID: " + id);
        }
        apiKeyRepository.deleteById(id);
    }

    private ApiKey findApiKeyOrThrow(Long id) {
        return apiKeyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy API Key với ID: " + id));
    }

    private void assertProviderExists(Long providerId) {
        if (!providerRepository.existsById(providerId)) {
            throw new ResourceNotFoundException("Không tìm thấy Provider với ID: " + providerId);
        }
    }
}
