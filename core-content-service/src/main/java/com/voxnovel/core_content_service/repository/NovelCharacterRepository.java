package com.voxnovel.core_content_service.repository;

import com.voxnovel.core_content_service.entity.NovelCharacter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NovelCharacterRepository extends JpaRepository<NovelCharacter, Long> {

    List<NovelCharacter> findByNovelId(Long novelId);

    @Query("""
            SELECT c FROM NovelCharacter c
            WHERE c.novel.id = :novelId
              AND (:name IS NULL OR :name = '' OR LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%')))
            ORDER BY c.name ASC
            """)
    List<NovelCharacter> searchByNovelIdAndName(
            @Param("novelId") Long novelId,
            @Param("name") String name);
}
