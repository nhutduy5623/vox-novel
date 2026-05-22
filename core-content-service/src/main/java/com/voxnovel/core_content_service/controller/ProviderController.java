package com.voxnovel.core_content_service.controller;

import com.voxnovel.core_content_service.dto.request.CreateProviderRequest;
import com.voxnovel.core_content_service.dto.response.ApiResponse;
import com.voxnovel.core_content_service.entity.Provider;
import com.voxnovel.core_content_service.service.ProviderService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/providers")
@RequiredArgsConstructor
public class ProviderController {

    private final ProviderService providerService;

    @GetMapping
    public ApiResponse<List<Provider>> getAll() {
        return ApiResponse.success(providerService.getAllProviders());
    }

    @PostMapping
    public ApiResponse<Provider> create(@Validated @RequestBody CreateProviderRequest request) {
        return ApiResponse.success(providerService.createProvider(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Provider> update(
            @PathVariable Long id,
            @Validated @RequestBody CreateProviderRequest request) {
        return ApiResponse.success(providerService.updateProvider(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        providerService.deleteProvider(id);
        return ApiResponse.successMessage("Đã xóa thành công!");
    }
}
