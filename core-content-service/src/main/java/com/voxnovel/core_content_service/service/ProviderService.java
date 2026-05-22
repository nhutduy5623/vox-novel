package com.voxnovel.core_content_service.service;

import com.voxnovel.core_content_service.dto.request.CreateProviderRequest;
import com.voxnovel.core_content_service.entity.Provider;
import com.voxnovel.core_content_service.exception.ResourceNotFoundException;
import com.voxnovel.core_content_service.mapper.SystemConfigMapper;
import com.voxnovel.core_content_service.repository.ProviderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProviderService {

    private final ProviderRepository providerRepository;
    private final SystemConfigMapper mapper;

    public Provider createProvider(CreateProviderRequest request) {
        Provider provider = mapper.toProviderEntity(request);
        return providerRepository.save(provider);
    }

    public Provider updateProvider(Long id, CreateProviderRequest request) {
        Provider provider = providerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Provider với ID: " + id));

        provider.setCode(request.getCode());
        provider.setName(request.getName());
        provider.setType(request.getType());

        return providerRepository.save(provider);
    }

    public void deleteProvider(Long id) {
        providerRepository.deleteById(id);
    }
    public List<Provider> getAllProviders() {
        return providerRepository.findAll();
    }
}