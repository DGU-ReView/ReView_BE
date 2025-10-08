package com.dgu.review.domain.interview.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dgu.review.domain.interview.entity.InterviewSession;

public interface InterviewSessionRepository extends JpaRepository<InterviewSession, Long>{
	boolean existsByIdAndUserId(Long id, Long userId);
}	
