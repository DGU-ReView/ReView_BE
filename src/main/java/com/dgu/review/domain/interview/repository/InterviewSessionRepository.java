package com.dgu.review.domain.interview.repository;


import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dgu.review.domain.interview.entity.InterviewSession;

public interface InterviewSessionRepository extends JpaRepository<InterviewSession, Long>{
	boolean existsByIdAndUserId(Long id, Long userId);
	
	@Query("""
        select s
        from InterviewSession s
        where s.user.id = :userId
          and (:cursor is null or s.id < :cursor)
        order by s.id desc
        """)
    List<InterviewSession> findSliceByUserId(@Param("userId")Long userId, @Param("cursor")Long cursor, Pageable pageable);
	Optional<InterviewSession> findByIdAndUserId(Long id, Long userId);
}
