package com.voxnovel.ai_engine_service.client;

import com.voxnovel.ai_engine_service.dto.request.AiScriptRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "core-content-service", url = "${feign.core-content-service.url}")
public interface CoreContentClient {
    @GetMapping("/internal/chapters/{chapterId}/ai-context")
    AiScriptRequest getChapterContext(@PathVariable("chapterId") Long chapterId);
}