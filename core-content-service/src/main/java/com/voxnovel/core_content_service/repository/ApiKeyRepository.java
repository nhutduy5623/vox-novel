package com.voxnovel.core_content_service.repository;

import com.voxnovel.core_content_service.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    List<ApiKey> findByProvider_IdOrderByIdDesc(Long providerId);

    List<ApiKey> findAllByOrderByIdDesc();

    boolean existsByKeyValue(String keyValue);

    Optional<ApiKey> findByKeyValue(String keyValue);
}
