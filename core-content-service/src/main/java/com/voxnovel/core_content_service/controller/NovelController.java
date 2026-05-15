package com.voxnovel.core_content_service.controller;

import com.voxnovel.core_content_service.dto.request.CreateNovelRequest;
import com.voxnovel.core_content_service.dto.response.NovelResponse;
import com.voxnovel.core_content_service.service.NovelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/novels")
@RequiredArgsConstructor
public class NovelController {

    private final NovelService novelService;

    @PostMapping
    public ResponseEntity<NovelResponse> create(@Validated @RequestBody CreateNovelRequest request) {
        // Sau này sẽ lấy từ Token, hiện tại gán cứng
        String currentUserId = "admin_01";
        return ResponseEntity.ok(novelService.createNovel(currentUserId, request));
    }

    @GetMapping
    public ResponseEntity<List<NovelResponse>> getAll() {
        return ResponseEntity.ok(novelService.getAllNovels());
    }

    @GetMapping("/{id}")
    public ResponseEntity<NovelResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(novelService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<NovelResponse> update(
            @PathVariable Long id,
            @Validated @RequestBody CreateNovelRequest request) {
        return ResponseEntity.ok(novelService.updateNovel(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        novelService.deleteNovel(id);
        return ResponseEntity.ok("Xóa truyện thành success!");
    }
}