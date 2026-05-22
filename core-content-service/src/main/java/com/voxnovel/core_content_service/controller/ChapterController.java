package com.voxnovel.core_content_service.controller;

import com.voxnovel.core_content_service.dto.message.ScriptLineDto;
import com.voxnovel.core_content_service.dto.request.CreateChapterRequest;
import com.voxnovel.core_content_service.dto.request.UpdateChapterCharactersRequest;
import com.voxnovel.core_content_service.dto.request.UpdateChapterRequest;
import com.voxnovel.core_content_service.dto.request.TriggerAiScriptRequest;
import com.voxnovel.core_content_service.dto.response.ApiResponse;
import com.voxnovel.core_content_service.dto.response.ChapterResponse;
import com.voxnovel.core_content_service.enums.ChapterStatus;
import com.voxnovel.core_content_service.service.ChapterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chapters")
@RequiredArgsConstructor
@Slf4j
public class ChapterController {

    private final ChapterService chapterService;

    @PostMapping
    public ApiResponse<ChapterResponse> create(@Validated @RequestBody CreateChapterRequest request) {
        String currentUserId = "admin_01";
        return ApiResponse.success(chapterService.createChapter(currentUserId, request));
    }

    @GetMapping("/{id}")
    public ApiResponse<ChapterResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(chapterService.getById(id));
    }

    @GetMapping("/novel/{novelId}")
    public ApiResponse<List<ChapterResponse>> getByNovel(@PathVariable Long novelId) {
        return ApiResponse.success(chapterService.getByNovel(novelId));
    }

    @PutMapping("/{id}")
    public ApiResponse<ChapterResponse> update(
            @PathVariable Long id,
            @Validated @RequestBody UpdateChapterRequest request) {
        return ApiResponse.success(chapterService.updateContent(id, request));
    }

    @PatchMapping("/{id}/characters")
    public ApiResponse<ChapterResponse> updateCharacters(
            @PathVariable Long id,
            @Validated @RequestBody UpdateChapterCharactersRequest request) {
        return ApiResponse.success(chapterService.updateChapterCharacters(id, request));
    }

    @PatchMapping("/{id}/script")
    public ApiResponse<ChapterResponse> updateScript(
            @PathVariable Long id,
            @RequestBody List<ScriptLineDto> scriptData) {
        return ApiResponse.success(
                chapterService.updateScript(id, scriptData, ChapterStatus.REVIEWING));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        chapterService.deleteChapter(id);
        return ApiResponse.successMessage("Xóa chương thành công!");
    }

    @PostMapping("/generate-script")
    public ApiResponse<Void> triggerAiGenerateScript(@Validated @RequestBody TriggerAiScriptRequest request) {
        chapterService.triggerAiScriptGeneration(request);
        return ApiResponse.successMessage("Gửi yêu cầu phân vai cho AI thành công! Hệ thống đang xử lý ngầm.");
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping("/{chapterId}/generate-audio")
    public ApiResponse<Void> generateChapterAudio(@PathVariable Long chapterId) {
        log.info("🎯 Nhận lệnh từ Client: Yêu cầu tạo Audio cho Chapter [{}]", chapterId);
        chapterService.requestAudioGeneration(chapterId);
        return ApiResponse.accepted("Đã đưa yêu cầu tạo Audio vào hàng đợi thành công!");
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping("/{chapterId}/merge-audio")
    public ApiResponse<Void> mergeChapterAudio(@PathVariable Long chapterId) {
        log.info("🎯 Nhận lệnh từ Client: Yêu cầu ghép Audio hoàn chỉnh cho Chapter [{}]", chapterId);
        chapterService.requestMergeChapterAudio(chapterId);
        return ApiResponse.accepted("Đã đưa yêu cầu ghép Audio vào hàng đợi thành công!");
    }

    @PatchMapping("/{chapterId}/audio")
    public ApiResponse<Void> updateChapterAudio(
            @PathVariable Long chapterId,
            @RequestParam String audioUrl) {
        log.info("🎯 Cập nhật audio cho Chapter [{}] với URL: {}", chapterId, audioUrl);
        chapterService.updateAudioStatus(chapterId, audioUrl);
        return ApiResponse.successMessage("Cập nhật audio thành công!");
    }
}
