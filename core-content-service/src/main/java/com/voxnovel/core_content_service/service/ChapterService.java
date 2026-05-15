package com.voxnovel.core_content_service.service;

import com.voxnovel.core_content_service.dto.message.AiScriptResponse;
import com.voxnovel.core_content_service.dto.message.ScriptLineDto;
import com.voxnovel.core_content_service.dto.request.CreateChapterRequest;
import com.voxnovel.core_content_service.dto.request.TriggerAiScriptRequest;
import com.voxnovel.core_content_service.dto.response.ChapterResponse;
import com.voxnovel.core_content_service.entity.Chapter;
import com.voxnovel.core_content_service.entity.Novel;
import com.voxnovel.core_content_service.entity.NovelCharacter;
import com.voxnovel.core_content_service.enums.ChapterStatus;
import com.voxnovel.core_content_service.mapper.ChapterMapper;
import com.voxnovel.core_content_service.rabbitmq.producer.AiScriptProducer;
import com.voxnovel.core_content_service.repository.ChapterRepository;
import com.voxnovel.core_content_service.repository.NovelCharacterRepository;
import com.voxnovel.core_content_service.repository.NovelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChapterService {

    private final ChapterRepository chapterRepository;
    private final NovelRepository novelRepository;
    private final ChapterMapper chapterMapper;
    private final NovelCharacterRepository characterRepository;

    @Transactional
    public ChapterResponse createChapter(String userId, CreateChapterRequest request) {
        Novel novel = novelRepository.findById(request.getNovelId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy truyện!"));

        Chapter chapter = chapterMapper.toEntity(request);
        chapter.setNovel(novel);
        chapter.setCreatedBy(userId);
        chapter.setHasAudio(false);
        chapter.setStatus(ChapterStatus.DRAFT);

        return chapterMapper.toResponse(chapterRepository.save(chapter));
    }

    public ChapterResponse getById(Long id) {
        return chapterRepository.findById(id)
                .map(chapterMapper::toResponse)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chương!"));
    }

    public List<ChapterResponse> getByNovel(Long novelId) {
        return chapterRepository.findByNovelIdOrderByChapterNumberAsc(novelId).stream()
                .map(chapterMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ChapterResponse updateContent(Long id, CreateChapterRequest request) {
        Chapter chapter = chapterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chương!"));

        chapter.setTitle(request.getTitle());
        chapter.setChapterNumber(request.getChapterNumber());
        chapter.setOriginalContent(request.getOriginalContent());

        return chapterMapper.toResponse(chapterRepository.save(chapter));
    }

    // Chức năng quan trọng: Cập nhật kịch bản từ Admin review hoặc AI
    @Transactional
    public ChapterResponse updateScript(Long id, List<ScriptLineDto> scriptData, ChapterStatus nextStatus) {
        Chapter chapter = chapterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chương!"));

        chapter.setScriptData(scriptData);
        if (nextStatus != null) {
            chapter.setStatus(nextStatus);
        }

        return chapterMapper.toResponse(chapterRepository.save(chapter));
    }

    @Transactional
    public void updateAudioStatus(Long id, String audioUrl) {
        Chapter chapter = chapterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chương!"));

        chapter.setAudioUrl(audioUrl);
        chapter.setHasAudio(true);
        chapter.setStatus(ChapterStatus.PUBLISHED);
        chapterRepository.save(chapter);
    }

    public void deleteChapter(Long id) {
        chapterRepository.deleteById(id);
    }


    //    Làm việc với các Service khác:
    private final AiScriptProducer aiScriptProducer;

    @Transactional
    public void triggerAiScriptGeneration(TriggerAiScriptRequest request) {
        Chapter chapter = chapterRepository.findById(request.getChapterId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chương!"));

        // 1. Dùng ArrayList mới để thoải mái remove/add mà không sợ lỗi Immutable List
        List<Long> processCharacterIds = new ArrayList<>(request.getCharacterIds());

        // 2. Lấy danh sách Narrator (Novel 0)
        List<NovelCharacter> narrators = characterRepository.findByNovelId(0L);
        Optional<NovelCharacter> foundNarrator = narrators.stream()
                .filter(n -> processCharacterIds.contains(n.getId()))
                .findFirst();

        NovelCharacter selectedNarrator;
        if (foundNarrator.isPresent()) {
            selectedNarrator = foundNarrator.get();
            processCharacterIds.remove(selectedNarrator.getId()); // Đã an toàn
        } else {
            selectedNarrator = narrators.get(0);
        }

        // 3. Gộp Narrator vào chung với dàn diễn viên thường
        processCharacterIds.add(selectedNarrator.getId());

        // 4. Lưu DB & Đổi trạng thái
        chapter.setCharacterIds(processCharacterIds);
        chapter.setStatus(ChapterStatus.PROCESSING_AI);
        chapterRepository.save(chapter);
        aiScriptProducer.sendGenerateScriptRequest(request.getChapterId());

        log.info("Đã chốt xong diễn viên (có Narrator) và gửi Queue cho chương: {}", chapter.getId());
    }

    @Transactional(readOnly = true)
    public AiScriptResponse prepareAiEngineResponse(Long chapterId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chương!"));

        // 1. Lấy TẤT CẢ nhân vật (Bao gồm cả diễn viên lẫn Narrator)
        List<NovelCharacter> allCharacters = characterRepository.findAllById(chapter.getCharacterIds());

        // 2. Lấy danh sách ID của các Narrator (thuộc Novel 0) làm "thước đo"
        // (Truy vấn này cực nhẹ và an toàn, không sợ lỗi Lazy Loading)
        List<Long> narratorIds = characterRepository.findByNovelId(0L).stream()
                .map(NovelCharacter::getId)
                .toList();

        // 3. Chuyển đổi sang định dạng của AI
        List<AiScriptResponse.CharacterInfo> characterInfos = allCharacters.stream()
                .map(c -> {
                    // Kiểm tra xem ID của nhân vật hiện tại có nằm trong nhóm Narrator không
                    boolean isNarrator = narratorIds.contains(c.getId());
                    return AiScriptResponse.CharacterInfo.builder()
                            .characterId(c.getId().toString())
                            .name(c.getName())
                            .gender(c.getGender())
                            // Nếu là Narrator thì set voice kịch tính, nếu không thì lấy description gốc
                            .voiceTone(isNarrator ? "Khách quan, miêu tả kịch tính" : c.getDescription())
                            .isNarrator(isNarrator)
                            .build();
                })
                .collect(Collectors.toList());

        // 4. Trả về cho AI Engine
        return AiScriptResponse.builder()
                .chapterId(chapter.getId().toString()) // Thêm tiền tố chap_ theo đúng format của AI
                .chapterText(chapter.getOriginalContent())
                .characters(characterInfos)
                .build();
    }

}