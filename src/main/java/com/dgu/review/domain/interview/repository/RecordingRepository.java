package com.dgu.review.domain.interview.repository;


import com.dgu.review.domain.interview.entity.FeedbackQuestion;
import com.dgu.review.domain.interview.entity.InterviewQuestion;
import com.dgu.review.domain.interview.entity.Recording;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;


public interface RecordingRepository extends JpaRepository<Recording, Long> {
    Optional<Recording> findByInterviewQuestion(InterviewQuestion interviewQuestion);
    Optional<Recording> findByFeedbackQuestion(FeedbackQuestion feedbackQuestion);
    // 전체 루트 녹음 갯수
    @Query("""
        SELECT COUNT(r)
        FROM Recording r
        JOIN r.interviewQuestion iq
        JOIN iq.interviewSession s
        WHERE iq.parentQuestion IS NULL
          AND s.user.id <> :currentUserId
          AND r.sttText IS NOT NULL
          AND r.sttText <> ''
    """)
    long countRootRecordingsExcludingUser(@Param("currentUserId") Long currentUserId);

    @Query("""
          select count(r)
          from Recording r
          join r.interviewQuestion iq
          join iq.interviewSession s
          where s.id = :sessionId and r.sttText is null
    """)
    int countTimeoutsBySessionId(@Param("sessionId") Long sessionId);

    // 특정 offset에서 1개만 조회
    @Query("""
        SELECT r
        FROM Recording r
        JOIN FETCH r.interviewQuestion iq
        JOIN FETCH iq.interviewSession s
        WHERE iq.parentQuestion IS NULL
          AND s.user.id <> :currentUserId
          AND r.sttText IS NOT NULL
          AND r.sttText <> ''
        ORDER BY r.id
    """)
    List<Recording> findRootRecordingAtOffset(@Param("currentUserId") Long currentUserId, Pageable pageable);

    //타인평가 시 이미 평가한 녹음 제외하고 랜덤 조회하는 쿼리
    @Query("""
        SELECT r
        FROM Recording r
        JOIN FETCH r.interviewQuestion iq
        JOIN FETCH iq.interviewSession s
        WHERE iq.parentQuestion IS NULL
          AND s.user.id <> :currentUserId
          AND r.sttText IS NOT NULL
          AND r.sttText <> ''
          AND r.id NOT IN (
              SELECT pf.recording.id
              FROM PeerFeedback pf
              WHERE pf.user.id = :currentUserId
          )
        ORDER BY r.id
    """)
    List<Recording> findRootRecordingExcludingUserAndAlreadyEvaluated(@Param("currentUserId") Long currentUserId,
                                                                      Pageable pageable);
}