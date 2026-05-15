package com.voxnovel.core_content_service.controller;

import com.voxnovel.core_content_service.dto.request.CreateVoiceRequest;
import com.voxnovel.core_content_service.service.VoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/voices")
@RequiredArgsConstructor
public class VoiceController {

    private final VoiceService voiceService;

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(voiceService.getAllVoices());
    }

    @PostMapping
    public ResponseEntity<?> create(@Validated @RequestBody CreateVoiceRequest request) {
        return ResponseEntity.ok(voiceService.createVoice(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Validated @RequestBody CreateVoiceRequest request) {
        return ResponseEntity.ok(voiceService.updateVoice(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        voiceService.deleteVoice(id);
        return ResponseEntity.ok("Đã xóa thành công!");
    }
}