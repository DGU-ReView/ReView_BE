package com.dgu.review.domain.interview.repository;

import com.dgu.review.domain.interview.entity.FeedbackQuestion;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FeedbackQuestionRepository extends JpaRepository<FeedbackQuestion, Long> {
	@Query("""
	        SELECT fq
	        FROM FeedbackQuestion fq
	        JOIN fq.interviewSession s
	        JOIN s.user u
	        LEFT JOIN FETCH fq.recording r
	        WHERE fq.parentQuestion.id = :questionId
	          AND u.id = :userId
	        ORDER BY fq.id ASC
	    """)
	    List<FeedbackQuestion> findAllByParentQuestionIdAndUserId(
	            @Param("questionId") Long questionId,
	            @Param("userId") Long userId
	    );
}
