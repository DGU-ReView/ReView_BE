package com.dgu.review.domain.interview.repository;

import com.dgu.review.domain.interview.entity.InterviewQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, Long> {
	boolean existsByIdAndInterviewSessionUserId(Long id, Long userId);

	@Query("""
        select count(rq)
        from InterviewQuestion rq
        where rq.interviewSession.id = :sessionId
          and rq.parentQuestion is null
          and (
                rq.aiFeedback is null or length(trim(rq.aiFeedback)) = 0
             or rq.selfFeedback is null or length(trim(rq.selfFeedback)) = 0
          )
    """)
	long countRootsMissingFeedback(@Param("sessionId") Long sessionId);

	@Query("""
        select 
            q.questionNumber as questionNumber,
            q.question       as rootQuestion,
            r.sttText        as sttText,
            q.aiFeedback     as aiFeedback,
            q.selfFeedback   as selfFeedback
        from InterviewQuestion q
        left join q.recording r
        where q.interviewSession.id = :sessionId
          and q.parentQuestion is null
        order by q.questionNumber asc, q.id asc
    """)
	List<QuestionSummaryView> findOrderedRootSummaries(@Param("sessionId") Long sessionId);

	interface QuestionSummaryView {
		Integer getQuestionNumber();
		String getRootQuestion();
		String getSttText();
		String getAiFeedback();
		String getSelfFeedback();
	}

	@Query("""
    SELECT iq
    FROM InterviewQuestion iq
    JOIN FETCH iq.interviewSession s
    LEFT JOIN FETCH iq.recording r
    LEFT JOIN FETCH iq.followUpQuestion f
    WHERE s.id = :sessionId
      AND iq.parentQuestion IS NULL
    ORDER BY iq.id
""")
	List<InterviewQuestion> findRootsBySessionId(@Param("sessionId") Long sessionId);
}