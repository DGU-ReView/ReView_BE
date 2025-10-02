package com.dgu.review.domain.interview.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dgu.review.domain.interview.entity.InterviewSession;

public interface InterviewSessionRepository extends JpaRepository<InterviewSession, Long>{
	boolean existsById(Long id);
	boolean existsByIdAndUser_Id(Long id, Long user_id);
}	
