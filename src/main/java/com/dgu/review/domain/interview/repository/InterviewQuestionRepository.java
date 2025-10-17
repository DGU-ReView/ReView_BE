package com.dgu.review.domain.interview.repository;

import com.dgu.review.domain.interview.entity.InterviewQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, Long> {
	boolean existsByIdAndInterviewSessionUserId(Long id, Long userId);

	@Query("""
    SELECT iq
    FROM InterviewQuestion iq
    JOIN FETCH iq.interviewSession s
    LEFT JOIN FETCH iq.recording r
    LEFT JOIN FETCH iq.followUpQuestion f
    WHERE s.id = :sessionId
      AND iq.parentQuestion IS NULL
    ORDER BY iq.questionNumber asc, iq.id asc
""")
	List<InterviewQuestion> findRootsBySessionId(@Param("sessionId") Long sessionId);
}