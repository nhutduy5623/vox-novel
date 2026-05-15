package com.voxnovel.core_content_service.controller;

import com.voxnovel.core_content_service.dto.request.CreateProviderRequest;
import com.voxnovel.core_content_service.service.ProviderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/providers")
@RequiredArgsConstructor
public class ProviderController {

    private final ProviderService providerService;

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(providerService.getAllProviders());
    }

    @PostMapping
    public ResponseEntity<?> create(@Validated @RequestBody CreateProviderRequest request) {
        return ResponseEntity.ok(providerService.createProvider(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Validated @RequestBody CreateProviderRequest request) {
        return ResponseEntity.ok(providerService.updateProvider(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        providerService.deleteProvider(id);
        return ResponseEntity.ok("Đã xóa thành công!");
    }
}