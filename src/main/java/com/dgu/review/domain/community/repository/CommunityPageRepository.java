package com.dgu.review.domain.community.repository;

import com.dgu.review.domain.community.entity.CommunityPage;
import com.dgu.review.domain.community.entity.DomainCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommunityPageRepository extends JpaRepository<CommunityPage, Long> {


    Page<CommunityPage> findAllByOrderByUpdatedAtDesc(Pageable pageable);

    @Query("""
           SELECT c
             FROM CommunityPage c
            WHERE LOWER(c.companyName) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(c.job)         LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(c.domain)      LIKE LOWER(CONCAT('%', :keyword, '%'))
           """)
    Page<CommunityPage> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);


    /**
     * 🔹 카테고리별 커서 기반 조회 (무한 스크롤용)
     *
     * @param category  DomainCategory (enum)
     * @param cursor    마지막 조회한 게시글 ID (null이면 최신부터)
     * @param limit     조회 개수
     * @return 지정한 카테고리의 최신 게시글 리스트
     */
    @Query(value = """
        SELECT *
          FROM community_page
         WHERE domain = :#{#category.name()}
           AND (:cursor IS NULL OR page_id < :cursor)
         ORDER BY page_id DESC
         LIMIT :limit
    """, nativeQuery = true)
    List<CommunityPage> findByCategoryWithCursor(
            @Param("category") DomainCategory category,
            @Param("cursor") Long cursor,
            @Param("limit") int limit
    );
}
