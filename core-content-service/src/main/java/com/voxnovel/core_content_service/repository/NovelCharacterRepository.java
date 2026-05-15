package com.voxnovel.core_content_service.repository;

import com.voxnovel.core_content_service.entity.NovelCharacter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NovelCharacterRepository extends JpaRepository<NovelCharacter, Long> {
     List<NovelCharacter> findByNovelId(Long novelId);
}