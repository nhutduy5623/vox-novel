package com.voxnovel.core_content_service.controller;

import com.voxnovel.core_content_service.dto.request.CreateApiKeyRequest;
import com.voxnovel.core_content_service.dto.request.UpdateApiKeyActiveRequest;
import com.voxnovel.core_content_service.dto.response.ApiKeyResponse;
import com.voxnovel.core_content_service.dto.response.ApiResponse;
import com.voxnovel.core_content_service.service.ApiKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/api-keys")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    @GetMapping
    public ApiResponse<List<ApiKeyResponse>> getAll(
            @RequestParam(required = false) Long providerId) {
        return ApiResponse.success(apiKeyService.getApiKeys(providerId));
    }

    @GetMapping("/{id}")
    public ApiResponse<ApiKeyResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(apiKeyService.getById(id));
    }

    @PostMapping
    public ApiResponse<ApiKeyResponse> create(@Validated @RequestBody CreateApiKeyRequest request) {
        String currentUserId = "admin_01";
        return ApiResponse.success(apiKeyService.createApiKey(currentUserId, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ApiKeyResponse> update(
            @PathVariable Long id,
            @Validated @RequestBody CreateApiKeyRequest request) {
        return ApiResponse.success(apiKeyService.updateApiKey(id, request));
    }

    @PatchMapping("/{id}/active")
    public ApiResponse<ApiKeyResponse> updateActive(
            @PathVariable Long id,
            @Validated @RequestBody UpdateApiKeyActiveRequest request) {
        return ApiResponse.success(apiKeyService.updateActiveStatus(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        apiKeyService.deleteApiKey(id);
        return ApiResponse.successMessage("Đã xóa API Key thành công!");
    }
}
