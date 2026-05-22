package com.voxnovel.core_content_service.service;

import com.voxnovel.core_content_service.dto.message.AiScriptResponse;
import com.voxnovel.core_content_service.dto.message.MergeAudioRequestMessage;
import com.voxnovel.core_content_service.dto.message.ScriptLineDto;
import com.voxnovel.core_content_service.dto.message.TtsScriptDataResponse;
import com.voxnovel.core_content_service.dto.request.CreateChapterRequest;
import com.voxnovel.core_content_service.dto.request.UpdateChapterCharactersRequest;
import com.voxnovel.core_content_service.dto.request.UpdateChapterRequest;
import com.voxnovel.core_content_service.dto.request.TriggerAiScriptRequest;
import com.voxnovel.core_content_service.dto.response.ChapterResponse;
import com.voxnovel.core_content_service.dto.response.NovelCharacterResponse;
import com.voxnovel.core_content_service.entity.Chapter;
import com.voxnovel.core_content_service.entity.Novel;
import com.voxnovel.core_content_service.entity.NovelCharacter;
import com.voxnovel.core_content_service.entity.Voice;
import com.voxnovel.core_content_service.enums.ChapterStatus;
import com.voxnovel.core_content_service.exception.BadRequestException;
import com.voxnovel.core_content_service.exception.ResourceNotFoundException;
import com.voxnovel.core_content_service.mapper.ChapterMapper;
import com.voxnovel.core_content_service.mapper.NovelCharacterMapper;
import com.voxnovel.core_content_service.rabbitmq.producer.AiScriptProducer;
import com.voxnovel.core_content_service.rabbitmq.producer.MergeAudioProducer;
import com.voxnovel.core_content_service.rabbitmq.producer.TtsMessageProducer;
import com.voxnovel.core_content_service.repository.ChapterRepository;
import com.voxnovel.core_content_service.repository.NovelCharacterRepository;
import com.voxnovel.core_content_service.repository.NovelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChapterService {

    private final ChapterRepository chapterRepository;
    private final NovelRepository novelRepository;
    private final ChapterMapper chapterMapper;
    private final NovelCharacterRepository characterRepository;
    private final NovelCharacterMapper characterMapper;
    private final MergeAudioProducer mergeAudioProducer;

    @Transactional
    public ChapterResponse createChapter(String userId, CreateChapterRequest request) {
        Novel novel = novelRepository.findById(request.getNovelId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy truyện với ID: " + request.getNovelId()));

        Chapter chapter = chapterMapper.toEntity(request);
        chapter.setNovel(novel);
        chapter.setCreatedBy(userId);
        chapter.setHasAudio(false);
        chapter.setStatus(ChapterStatus.DRAFT);

        return chapterMapper.toResponse(chapterRepository.save(chapter));
    }

    @Transactional(readOnly = true)
    public ChapterResponse getById(Long id) {
        Chapter chapter = chapterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chương với ID: " + id));

        ChapterResponse response = chapterMapper.toResponse(chapter);
        response.setCharacters(resolveChapterCharacters(chapter.getCharacterIds()));
        return response;
    }

    private List<NovelCharacterResponse> resolveChapterCharacters(List<Long> characterIds) {
        if (characterIds == null || characterIds.isEmpty()) {
            return List.of();
        }

        Map<Long, NovelCharacter> characterById = characterRepository.findAllById(characterIds).stream()
                .collect(Collectors.toMap(NovelCharacter::getId, c -> c, (a, b) -> a));

        return characterIds.stream()
                .map(characterById::get)
                .filter(Objects::nonNull)
                .map(characterMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<ChapterResponse> getByNovel(Long novelId) {
        return chapterRepository.findByNovelIdOrderByChapterNumberAsc(novelId).stream()
                .map(chapterMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ChapterResponse updateContent(Long id, UpdateChapterRequest request) {
        Chapter chapter = chapterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chương với ID: " + id));

        chapter.setTitle(request.getTitle());
        chapter.setChapterNumber(request.getChapterNumber());
        chapter.setOriginalContent(request.getOriginalContent());
        if (request.getStatus() != null) {
            chapter.setStatus(request.getStatus());
        }

        return chapterMapper.toResponse(chapterRepository.save(chapter));
    }

    @Transactional
    public ChapterResponse updateChapterCharacters(Long id, UpdateChapterCharactersRequest request) {
        Chapter chapter = chapterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chương với ID: " + id));

        Long novelId = chapter.getNovel().getId();
        List<Long> ids = request.getCharacterIds() == null
                ? new ArrayList<>()
                : new ArrayList<>(request.getCharacterIds());

        if (!ids.isEmpty()) {
            List<NovelCharacter> found = characterRepository.findAllById(ids);
            if (found.size() != new HashSet<>(ids).size()) {
                throw new BadRequestException("Một hoặc nhiều nhân vật không tồn tại.");
            }
            for (NovelCharacter character : found) {
                if (!novelId.equals(character.getNovel().getId()) && character.getNovel().getId() != 0L) {
                    throw new BadRequestException(
                            "Nhân vật ID " + character.getId() + " không thuộc truyện này.");
                }
            }
        }

        chapter.setCharacterIds(ids);
        Chapter saved = chapterRepository.save(chapter);
        ChapterResponse response = chapterMapper.toResponse(saved);
        response.setCharacters(resolveChapterCharacters(ids));
        return response;
    }

    // Chức năng quan trọng: Cập nhật kịch bản từ Admin review hoặc AI
    @Transactional
    public ChapterResponse updateScript(Long id, List<ScriptLineDto> scriptData, ChapterStatus nextStatus) {
        Chapter chapter = chapterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chương với ID: " + id));

        chapter.setScriptData(scriptData);
        if (nextStatus != null) {
            chapter.setStatus(nextStatus);
        }

        return chapterMapper.toResponse(chapterRepository.save(chapter));
    }

    @Transactional
    public void updateAudioStatus(Long id, String audioUrl) {
        Chapter chapter = chapterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chương với ID: " + id));

        chapter.setAudioUrl(audioUrl);
        chapter.setHasAudio(true);
        chapter.setStatus(ChapterStatus.PUBLISHED);
        chapterRepository.save(chapter);
    }

    @Transactional
    public void requestMergeChapterAudio(Long chapterId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chương với ID: " + chapterId));

        Long novelId = chapter.getNovel().getId();

        chapter.setStatus(ChapterStatus.PROCESSING_AI);
        chapterRepository.save(chapter);

        mergeAudioProducer.sendMergeChapterRequest(
                new MergeAudioRequestMessage(String.valueOf(novelId), String.valueOf(chapterId)));
    }

    public void deleteChapter(Long id) {
        chapterRepository.deleteById(id);
    }


    //    Làm việc với các Service khác:
    private final AiScriptProducer aiScriptProducer;

    @Transactional
    public void triggerAiScriptGeneration(TriggerAiScriptRequest request) {
        Chapter chapter = chapterRepository.findById(request.getChapterId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy chương với ID: " + request.getChapterId()));

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
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chương với ID: " + chapterId));

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


    //    TTS
    private final TtsMessageProducer ttsMessageProducer;
    @Transactional
    public void requestAudioGeneration(Long chapterId) {
        log.info("⚙️ Bắt đầu kiểm tra và xử lý yêu cầu tạo Audio cho Chapter [{}]", chapterId);

        // 1. Kiểm tra tồn tại
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chương với ID: " + chapterId));

        if (chapter.getScriptData() == null || chapter.getScriptData().isEmpty()) {
            log.warn("⚠️ Chapter [{}] chưa có kịch bản JSON. Từ chối yêu cầu.", chapterId);
            throw new BadRequestException(
                    "Chapter này chưa có kịch bản JSON. Vui lòng chạy phân vai AI trước!");
        }

        // 3. Đổi trạng thái Chapter để UI hiện chữ "Đang tạo Audio..."
         chapter.setStatus(ChapterStatus.PROCESSING_AI);
         chapterRepository.save(chapter);

        // 4. Kích hoạt Producer ném lệnh sang Media & TTS Service
        ttsMessageProducer.sendTtsGenerateRequest(chapterId);

        log.info("✅ Đã hoàn tất đẩy yêu cầu tạo Audio Chapter [{}] sang hệ thống TTS.", chapterId);
    }


    @Transactional(readOnly = true)
    public TtsScriptDataResponse buildScriptDataForTts(Long chapterId) {
        log.info("🔍 Lấy dữ liệu kịch bản chi tiết chuẩn bị gửi cho TTS Service. Chapter ID: {}", chapterId);

        // 1. Tìm Chapter trong Database
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chương với ID: " + chapterId));

        if (chapter.getNovel() == null) {
            throw new BadRequestException("Chapter ID " + chapterId + " không gắn liền với Novel nào!");
        }

        Long novelId = chapter.getNovel().getId();

        // 2. Tối ưu hiệu năng (Tránh lỗi N+1): Gom toàn bộ Diễn viên của Truyện này vào Map trước
        List<NovelCharacter> NarratorList = characterRepository.findByNovelId(0L);
        List<NovelCharacter> characterList = characterRepository.findByNovelId(novelId);
        characterList.addAll(NarratorList);

        log.info("characterList: [{}]", characterList);

        // Key của map: Ta hỗ trợ cả dạng String ID ("1") hoặc nếu lưu dạng "char_narrator_00"
        // thì map linh hoạt dựa theo logic định danh của hệ thống.
        Map<String, NovelCharacter> characterMap = characterList.stream()
                .collect(Collectors.toMap(
                        c -> String.valueOf(c.getId()),
                        c -> c,
                        (existing, replacement) -> existing // Phòng hờ trùng lặp dữ liệu bậy
                ));

        // 3. Nếu Chapter chưa phân vai / chưa có kịch bản JSON, trả về danh sách rỗng để tránh crash mượt mà
        if (chapter.getScriptData() == null || chapter.getScriptData().isEmpty()) {
            log.warn("⚠️ Chapter [{}] hiện chưa có kịch bản phân vai (scriptData trống).", chapterId);
            return TtsScriptDataResponse.builder()
                    .novelId(String.valueOf(novelId))
                    .chapterId(String.valueOf(chapterId))
                    .scriptLines(Collections.emptyList())
                    .build();
        }

        // 4. Duyệt từng dòng kịch bản nháp và mapping lấy Voice ID, Provider thực tế
        List<TtsScriptDataResponse.TtsScriptLine> processedLines = chapter.getScriptData().stream()
                .map(lineDto -> {
                    String charIdInScript = lineDto.getCharacterId();

                    // Khai báo các giá trị mặc định nếu nhân vật "vô danh" hoặc bị xóa mất
                    String voiceId = "leminh";
                    String providerName = "FPT";

                    // Tìm kiếm thông tin Diễn viên trong Map đã gom sẵn
                    NovelCharacter character = characterMap.get(charIdInScript);

                    // Nếu không tìm thấy bằng ID dạng số, thử parse nếu script lưu chuỗi "char_123"
                    if (character == null && charIdInScript != null) {
                        character = characterMap.get(charIdInScript);
                    }

                    // Kỹ thuật phòng thủ lỗi NPE
                    if (character != null) {
                        Voice voice = character.getDefaultVoice();
                        if (voice != null) {
                            // Lấy trúng Provider Voice ID (vd: alloy, leminh, echo...)
                            voiceId = voice.getProviderVoiceId();

                            if (voice.getProvider() != null) {
                                // Lấy tên nhà cung cấp (vd: FPT, OPENAI, CAMBAI...)
                                providerName = voice.getProvider().getName();
                            }
                        }
                    } else {
                        log.warn("🕵️‍♂️ Không tìm thấy nhân vật tương ứng với characterId [{}] trong kịch bản chương {}", charIdInScript, chapterId);
                    }

                    // Build cục data hoàn chỉnh cho từng dòng thoại
                    return TtsScriptDataResponse.TtsScriptLine.builder()
                            .sequence(lineDto.getSequence())
                            .characterId(charIdInScript)
                            .voiceId(voiceId)
                            .provider(providerName)
                            .text(lineDto.getText())
                            .build();
                })
                .collect(Collectors.toList());

        log.info("✅ Build thành công cấu trúc kịch bản gửi đi cho TTS. Tổng số dòng thoại: {}", processedLines.size());

        return TtsScriptDataResponse.builder()
                .novelId(String.valueOf(novelId))
                .chapterId(String.valueOf(chapterId))
                .scriptLines(processedLines)
                .build();
    }

}
