package com.voxnovel.core_content_service.repository;

import com.voxnovel.core_content_service.entity.Novel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NovelRepository extends JpaRepository<Novel, Long> {

    @Query("""
            SELECT n FROM Novel n
            WHERE (:keyword IS NULL OR :keyword = '' OR LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<Novel> searchByTitleKeyword(@Param("keyword") String keyword, Pageable pageable);
}
