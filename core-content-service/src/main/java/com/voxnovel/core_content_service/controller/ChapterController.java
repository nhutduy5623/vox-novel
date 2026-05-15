package com.voxnovel.core_content_service.controller;

import com.voxnovel.core_content_service.dto.message.ScriptLineDto;
import com.voxnovel.core_content_service.dto.request.CreateChapterRequest;
import com.voxnovel.core_content_service.dto.request.TriggerAiScriptRequest;
import com.voxnovel.core_content_service.dto.response.ChapterResponse;
import com.voxnovel.core_content_service.enums.ChapterStatus;
import com.voxnovel.core_content_service.service.ChapterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chapters")
@RequiredArgsConstructor
public class ChapterController {

    private final ChapterService chapterService;

    @PostMapping
    public ResponseEntity<ChapterResponse> create(@Validated @RequestBody CreateChapterRequest request) {
        // Tạm thời hardcode userId, sau này lấy từ SecurityContext
        String currentUserId = "admin_01";
        return ResponseEntity.ok(chapterService.createChapter(currentUserId, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChapterResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(chapterService.getById(id));
    }

    @GetMapping("/novel/{novelId}")
    public ResponseEntity<List<ChapterResponse>> getByNovel(@PathVariable Long novelId) {
        return ResponseEntity.ok(chapterService.getByNovel(novelId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ChapterResponse> update(@PathVariable Long id, @Validated @RequestBody CreateChapterRequest request) {
        return ResponseEntity.ok(chapterService.updateContent(id, request));
    }

    // API để Admin lưu lại kịch bản sau khi chỉnh sửa
    @PatchMapping("/{id}/script")
    public ResponseEntity<ChapterResponse> updateScript(
            @PathVariable Long id,
            @RequestBody List<ScriptLineDto> scriptData) {
        return ResponseEntity.ok(chapterService.updateScript(id, scriptData, ChapterStatus.REVIEWING));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        chapterService.deleteChapter(id);
        return ResponseEntity.ok("Xóa chương thành công!");
    }


    @PostMapping("/generate-script")
    public ResponseEntity<?> triggerAiGenerateScript(@Validated @RequestBody TriggerAiScriptRequest request) {
        chapterService.triggerAiScriptGeneration(request);
        return ResponseEntity.ok("Gửi yêu cầu phân vai cho AI thành công! Hệ thống đang xử lý ngầm.");
    }
}