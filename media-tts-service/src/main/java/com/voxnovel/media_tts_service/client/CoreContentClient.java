package com.voxnovel.media_tts_service.client;

import com.voxnovel.media_tts_service.dto.request.AudioDraftRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// Tên "core-content-service" phải khớp với tên khai báo trong Eureka của Core
@FeignClient(name = "core-content-service", url = "${feign.core-content-service.url}")
public interface CoreContentClient {

    @GetMapping("/internal/chapters/script/{chapterId}")
    AudioDraftRequest getScriptForTts(@PathVariable("chapterId") Long chapterId);
}