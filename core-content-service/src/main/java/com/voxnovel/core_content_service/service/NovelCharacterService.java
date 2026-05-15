package com.voxnovel.core_content_service.service;

import com.voxnovel.core_content_service.dto.request.CreateCharacterRequest;
import com.voxnovel.core_content_service.dto.response.NovelCharacterResponse;
import com.voxnovel.core_content_service.entity.Novel;
import com.voxnovel.core_content_service.entity.NovelCharacter;
import com.voxnovel.core_content_service.entity.Voice;
import com.voxnovel.core_content_service.mapper.NovelCharacterMapper;
import com.voxnovel.core_content_service.repository.NovelCharacterRepository;
import com.voxnovel.core_content_service.repository.NovelRepository;
import com.voxnovel.core_content_service.repository.VoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NovelCharacterService {

    private final NovelCharacterRepository characterRepository;
    private final NovelRepository novelRepository;
    private final VoiceRepository voiceRepository;
    private final NovelCharacterMapper characterMapper;

    @Transactional
    public NovelCharacterResponse createCharacter(CreateCharacterRequest request) {
        Novel novel = novelRepository.findById(request.getNovelId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy truyện với ID: " + request.getNovelId()));

        NovelCharacter character = characterMapper.toEntity(request);
        character.setNovel(novel);

        // Gán giọng mặc định nếu có
        if (request.getDefaultVoiceId() != null) {
            Voice voice = voiceRepository.findById(request.getDefaultVoiceId()).orElse(null);
            character.setDefaultVoice(voice);
        }

        NovelCharacter saved = characterRepository.save(character);
        return characterMapper.toResponse(saved);
    }

    @Transactional
    public NovelCharacterResponse updateCharacter(Long id, CreateCharacterRequest request) {
        NovelCharacter character = characterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân vật với ID: " + id));

        // Cập nhật các trường cơ bản
        character.setName(request.getName());
        character.setGender(request.getGender());
        character.setDescription(request.getDescription());

        // Cập nhật giọng mặc định
        if (request.getDefaultVoiceId() != null) {
            Voice voice = voiceRepository.findById(request.getDefaultVoiceId()).orElse(null);
            character.setDefaultVoice(voice);
        } else {
            character.setDefaultVoice(null);
        }

        return characterMapper.toResponse(characterRepository.save(character));
    }

    public List<NovelCharacterResponse> getAllByNovel(Long novelId) {
//       findByNovelId vào Repository
         return characterRepository.findByNovelId(novelId).stream()
                 .map(characterMapper::toResponse)
                 .collect(Collectors.toList());

        // Hiện tại dùng tạm findAll
//        return characterRepository.findAll().stream()
//                .map(characterMapper::toResponse)
//                .collect(Collectors.toList());
    }

    public void deleteCharacter(Long id) {
        characterRepository.deleteById(id);
    }
}