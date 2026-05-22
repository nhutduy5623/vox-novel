package com.voxnovel.core_content_service.controller;

import com.voxnovel.core_content_service.dto.request.CreateVoiceRequest;
import com.voxnovel.core_content_service.dto.response.ApiResponse;
import com.voxnovel.core_content_service.dto.response.VoiceResponse;
import com.voxnovel.core_content_service.service.VoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/voices")
@RequiredArgsConstructor
public class VoiceController {

    private final VoiceService voiceService;

    @GetMapping
    public ApiResponse<List<VoiceResponse>> getAll(
            @RequestParam(required = false) Long providerId) {
        return ApiResponse.success(voiceService.getVoices(providerId));
    }

    @PostMapping
    public ApiResponse<VoiceResponse> create(@Validated @RequestBody CreateVoiceRequest request) {
        return ApiResponse.success(voiceService.createVoice(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<VoiceResponse> update(
            @PathVariable Long id,
            @Validated @RequestBody CreateVoiceRequest request) {
        return ApiResponse.success(voiceService.updateVoice(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        voiceService.deleteVoice(id);
        return ApiResponse.successMessage("Đã xóa thành công!");
    }
}
