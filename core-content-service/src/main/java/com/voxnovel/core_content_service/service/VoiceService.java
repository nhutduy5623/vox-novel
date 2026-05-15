package com.voxnovel.core_content_service.service;

import com.voxnovel.core_content_service.dto.request.CreateVoiceRequest;
import com.voxnovel.core_content_service.dto.response.VoiceResponse;
import com.voxnovel.core_content_service.entity.Provider;
import com.voxnovel.core_content_service.entity.Voice;
import com.voxnovel.core_content_service.mapper.VoiceMapper;
import com.voxnovel.core_content_service.repository.ProviderRepository;
import com.voxnovel.core_content_service.repository.VoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VoiceService {

    private final VoiceRepository voiceRepository;
    private final ProviderRepository providerRepository;
    private final VoiceMapper voiceMapper;

    public VoiceResponse createVoice(CreateVoiceRequest request) {
        Provider provider = providerRepository.findById(request.getProviderId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Provider"));

        Voice voice = voiceMapper.toEntity(request);
        voice.setProvider(provider);
        return voiceMapper.toResponse(voiceRepository.save(voice));
    }

    public VoiceResponse updateVoice(Long id, CreateVoiceRequest request) {
        Voice voice = voiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Voice"));

        Provider provider = providerRepository.findById(request.getProviderId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Provider"));

        voice.setProvider(provider);
        voice.setProviderVoiceId(request.getProviderVoiceId());
        voice.setName(request.getName());
        voice.setGender(request.getGender());
        voice.setLanguage(request.getLanguage());
        voice.setPreviewUrl(request.getPreviewUrl());

        return voiceMapper.toResponse(voiceRepository.save(voice));
    }

    public void deleteVoice(Long id) {
        voiceRepository.deleteById(id);
    }

    public List<VoiceResponse> getAllVoices() {
        return voiceRepository.findAll().stream()
                .map(voiceMapper::toResponse)
                .collect(Collectors.toList());
    }
}