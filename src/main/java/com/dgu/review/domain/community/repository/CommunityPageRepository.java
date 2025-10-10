package com.dgu.review.domain.community.repository;


import com.dgu.review.domain.community.entity.CommunityPage;
import com.dgu.review.domain.community.entity.DomainCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommunityPageRepository extends JpaRepository<CommunityPage, Long> {


    Page<CommunityPage> findAllByOrderByCreatedAtDesc(Pageable pageable);


    @Query("""
           SELECT c
             FROM CommunityPage c
            WHERE LOWER(c.companyName) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(c.job)         LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(c.domain)      LIKE LOWER(CONCAT('%', :keyword, '%'))
           """)
    Page<CommunityPage> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

}
