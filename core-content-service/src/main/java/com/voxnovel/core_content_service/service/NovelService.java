package com.voxnovel.core_content_service.service;

import com.voxnovel.core_content_service.dto.request.CreateNovelRequest;
import com.voxnovel.core_content_service.dto.response.NovelResponse;
import com.voxnovel.core_content_service.entity.Novel;
import com.voxnovel.core_content_service.mapper.NovelMapper;
import com.voxnovel.core_content_service.repository.NovelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NovelService {

    private final NovelRepository novelRepository;
    private final NovelMapper novelMapper;

    @Transactional
    public NovelResponse createNovel(String userId, CreateNovelRequest request) {
        log.info("User {} đang tạo truyện mới: {}", userId, request.getTitle());
        Novel novel = novelMapper.toEntity(request);
        novel.setCreatedBy(userId);

        return novelMapper.toResponse(novelRepository.save(novel));
    }

    public List<NovelResponse> getAllNovels() {
        return novelRepository.findAll().stream()
                .map(novelMapper::toResponse)
                .collect(Collectors.toList());
    }

    public NovelResponse getById(Long id) {
        return novelRepository.findById(id)
                .map(novelMapper::toResponse)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bộ truyện ID: " + id));
    }

    @Transactional
    public NovelResponse updateNovel(Long id, CreateNovelRequest request) {
        Novel novel = novelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bộ truyện!"));

        novel.setTitle(request.getTitle());
        novel.setDescription(request.getDescription());
        novel.setCoverImageUrl(request.getCoverImageUrl());
        novel.setStatus(request.getStatus());

        return novelMapper.toResponse(novelRepository.save(novel));
    }

    @Transactional
    public void deleteNovel(Long id) {
        novelRepository.deleteById(id);
    }
}
