package com.voxnovel.core_content_service.controller;

import com.voxnovel.core_content_service.dto.request.CreateCharacterRequest;
import com.voxnovel.core_content_service.dto.response.NovelCharacterResponse;
import com.voxnovel.core_content_service.service.NovelCharacterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/characters")
@RequiredArgsConstructor
public class NovelCharacterController {

    private final NovelCharacterService characterService;

    @PostMapping
    public ResponseEntity<NovelCharacterResponse> create(@Validated @RequestBody CreateCharacterRequest request) {
        return ResponseEntity.ok(characterService.createCharacter(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<NovelCharacterResponse> update(@PathVariable Long id, @Validated @RequestBody CreateCharacterRequest request) {
        return ResponseEntity.ok(characterService.updateCharacter(id, request));
    }

    // ... các API khác ...
}
