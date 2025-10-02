package com.dgu.review.domain.interview.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dgu.review.domain.interview.entity.InterviewQuestion;

public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, Long> {
	boolean existsById(Long id);
	boolean existsByIdAndInterviewSession_User_Id(Long id, Long userId);
}

