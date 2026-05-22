package com.voxnovel.core_content_service.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Slf4j
@RestController
@RequestMapping("/api/media")
public class MediaProxyController {

    @Value("${services.media-tts.base-url}")
    private String mediaTtsBaseUrl;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @GetMapping("/audio/{bucket}/{*path}")
    public ResponseEntity<byte[]> proxyAudio(
            @PathVariable String bucket,
            @PathVariable String path
    ) {
        String cleanPath = path != null && path.startsWith("/") ? path.substring(1) : path;
        String targetUrl = String.format("%s/api/v1/media/audio/%s/%s", mediaTtsBaseUrl, bucket, cleanPath);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .GET()
                    .build();

            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return ResponseEntity.status(response.statusCode()).build();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("audio/mpeg"));
            headers.setContentLength(response.body().length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(response.body());
        } catch (Exception e) {
            log.error("Proxy audio failed: {}", targetUrl, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

