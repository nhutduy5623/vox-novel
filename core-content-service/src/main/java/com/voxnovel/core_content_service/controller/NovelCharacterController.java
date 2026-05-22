package com.voxnovel.core_content_service.controller;

import com.voxnovel.core_content_service.dto.request.CreateCharacterRequest;
import com.voxnovel.core_content_service.dto.response.ApiResponse;
import com.voxnovel.core_content_service.dto.response.NovelCharacterResponse;
import com.voxnovel.core_content_service.service.NovelCharacterService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/characters")
@RequiredArgsConstructor
public class NovelCharacterController {

    private final NovelCharacterService characterService;

    @PostMapping
    public ApiResponse<NovelCharacterResponse> create(@Validated @RequestBody CreateCharacterRequest request) {
        return ApiResponse.success(characterService.createCharacter(request));
    }

    @GetMapping("/search")
    public ApiResponse<List<NovelCharacterResponse>> search(
            @RequestParam Long novelId,
            @RequestParam(required = false) String name) {
        return ApiResponse.success(characterService.searchCharacters(novelId, name));
    }

    @GetMapping("/novel/{novelId}")
    public ApiResponse<List<NovelCharacterResponse>> getByNovel(@PathVariable Long novelId) {
        return ApiResponse.success(characterService.getAllByNovel(novelId));
    }

    @PutMapping("/{id}")
    public ApiResponse<NovelCharacterResponse> update(
            @PathVariable Long id,
            @Validated @RequestBody CreateCharacterRequest request) {
        return ApiResponse.success(characterService.updateCharacter(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        characterService.deleteCharacter(id);
        return ApiResponse.successMessage("Đã xóa nhân vật thành công!");
    }
}
