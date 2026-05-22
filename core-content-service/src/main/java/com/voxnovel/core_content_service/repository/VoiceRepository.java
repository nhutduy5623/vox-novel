package com.voxnovel.core_content_service.repository;

import com.voxnovel.core_content_service.entity.Voice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VoiceRepository extends JpaRepository<Voice, Long> {

    List<Voice> findByProvider_IdOrderByNameAsc(Long providerId);
}