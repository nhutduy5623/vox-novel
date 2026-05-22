package com.voxnovel.core_content_service.controller;

import com.voxnovel.core_content_service.dto.request.CreateNovelRequest;
import com.voxnovel.core_content_service.dto.response.ApiResponse;
import com.voxnovel.core_content_service.dto.response.NovelDetailResponse;
import com.voxnovel.core_content_service.dto.response.NovelResponse;
import com.voxnovel.core_content_service.service.NovelService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/novels")
@RequiredArgsConstructor
public class NovelController {

    private final NovelService novelService;

    @PostMapping
    public ApiResponse<NovelResponse> create(@Validated @RequestBody CreateNovelRequest request) {
        String currentUserId = "admin_01";
        return ApiResponse.success(novelService.createNovel(currentUserId, request));
    }

    @GetMapping
    public ApiResponse<List<NovelResponse>> getAll() {
        return ApiResponse.success(novelService.getAllNovels());
    }

    @GetMapping("/paged")
    public ApiResponse<Page<NovelResponse>> getPaged(
            @RequestParam(required = false) String title,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ApiResponse.success(novelService.searchNovels(title, pageable));
    }

    @GetMapping("/detail/{novelId}")
    public ApiResponse<NovelDetailResponse> getDetail(@PathVariable Long novelId) {
        return ApiResponse.success(novelService.getNovelDetail(novelId));
    }

    @GetMapping("/{id}")
    public ApiResponse<NovelResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(novelService.getById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<NovelResponse> update(
            @PathVariable Long id,
            @Validated @RequestBody CreateNovelRequest request) {
        return ApiResponse.success(novelService.updateNovel(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        novelService.deleteNovel(id);
        return ApiResponse.successMessage("Xóa truyện thành công!");
    }
}
