package com.voxnovel.core_content_service.repository;

import com.voxnovel.core_content_service.entity.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, Long> {
    // Tìm tất cả chương của một bộ truyện
    List<Chapter> findByNovelIdOrderByChapterNumberAsc(Long novelId);

    // Tìm một chương cụ thể của truyện
    Optional<Chapter> findByNovelIdAndChapterNumber(Long novelId, Integer chapterNumber);
}