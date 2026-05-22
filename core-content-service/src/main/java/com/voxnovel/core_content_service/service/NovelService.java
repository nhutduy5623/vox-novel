package com.voxnovel.core_content_service.service;

import com.voxnovel.core_content_service.dto.request.CreateNovelRequest;
import com.voxnovel.core_content_service.exception.ResourceNotFoundException;
import com.voxnovel.core_content_service.dto.response.NovelDetailChapterItem;
import com.voxnovel.core_content_service.dto.response.NovelDetailCharacterItem;
import com.voxnovel.core_content_service.dto.response.NovelDetailResponse;
import com.voxnovel.core_content_service.dto.response.NovelResponse;
import com.voxnovel.core_content_service.entity.Chapter;
import com.voxnovel.core_content_service.entity.Novel;
import com.voxnovel.core_content_service.entity.NovelCharacter;
import com.voxnovel.core_content_service.entity.Voice;
import com.voxnovel.core_content_service.mapper.NovelMapper;
import com.voxnovel.core_content_service.repository.ChapterRepository;
import com.voxnovel.core_content_service.repository.NovelCharacterRepository;
import com.voxnovel.core_content_service.repository.NovelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NovelService {

    private final NovelRepository novelRepository;
    private final ChapterRepository chapterRepository;
    private final NovelCharacterRepository characterRepository;
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

    @Transactional(readOnly = true)
    public Page<NovelResponse> searchNovels(String titleKeyword, Pageable pageable) {
        String keyword = normalizeKeyword(titleKeyword);
        return novelRepository.searchByTitleKeyword(keyword, pageable)
                .map(novelMapper::toResponse);
    }

    public NovelResponse getById(Long id) {
        return novelRepository.findById(id)
                .map(novelMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy truyện với ID: " + id));
    }

    @Transactional
    public NovelResponse updateNovel(Long id, CreateNovelRequest request) {
        Novel novel = novelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy truyện với ID: " + id));

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

    @Transactional(readOnly = true)
    public NovelDetailResponse getNovelDetail(Long novelId) {
        Novel novel = novelRepository.findById(novelId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy truyện với ID: " + novelId));

        NovelResponse base = novelMapper.toResponse(novel);

        List<NovelDetailChapterItem> chapters = chapterRepository
                .findByNovelIdOrderByChapterNumberAsc(novelId)
                .stream()
                .map(this::toDetailChapterItem)
                .collect(Collectors.toList());

        List<NovelDetailCharacterItem> characters = characterRepository
                .findByNovelId(novelId)
                .stream()
                .map(this::toDetailCharacterItem)
                .collect(Collectors.toList());

        return NovelDetailResponse.builder()
                .id(base.getId())
                .title(base.getTitle())
                .description(base.getDescription())
                .coverImageUrl(base.getCoverImageUrl())
                .status(base.getStatus())
                .createdBy(base.getCreatedBy())
                .createdAt(base.getCreatedAt())
                .updatedAt(base.getUpdatedAt())
                .chapters(chapters)
                .characters(characters)
                .build();
    }

    private NovelDetailChapterItem toDetailChapterItem(Chapter chapter) {
        return NovelDetailChapterItem.builder()
                .id(chapter.getId())
                .chapterNumber(chapter.getChapterNumber())
                .status(chapter.getStatus())
                .build();
    }

    private NovelDetailCharacterItem toDetailCharacterItem(NovelCharacter character) {
        Voice defaultVoice = character.getDefaultVoice();
        String previewUrl = defaultVoice != null ? defaultVoice.getPreviewUrl() : null;

        return NovelDetailCharacterItem.builder()
                .id(character.getId())
                .name(character.getName())
                .gender(character.getGender())
                .previewUrl(previewUrl)
                .build();
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
