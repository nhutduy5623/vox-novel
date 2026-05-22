package com.voxnovel.core_content_service.controller.internal;

import com.voxnovel.core_content_service.dto.message.TtsScriptDataResponse;
import com.voxnovel.core_content_service.service.ChapterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/chapters")
@RequiredArgsConstructor
public class InternalTtsController {
    private final ChapterService internalTtsService;

    // TTS Service sẽ dùng FeignClient gọi vào endpoint này
    @GetMapping("/script/{chapterId}")
    public TtsScriptDataResponse getScriptForTts(@PathVariable Long chapterId) {
        return internalTtsService.buildScriptDataForTts(chapterId);
    }
}