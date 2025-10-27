package com.dgu.review.domain.peerfeedback.repository;


import com.dgu.review.domain.peerfeedback.entity.PeerFeedback;
import com.dgu.review.domain.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PeerFeedbackRepository extends JpaRepository<PeerFeedback, Long> {

    List<PeerFeedback> findAllByUser(User user);

    @Query("""
        select pf.body
        from PeerFeedback pf
        where pf.user.id = :writerId
        order by pf.createdAt desc
    """)
    List<String> findRecentContentsByWriter(Long writerId, Pageable pageable);
    List<PeerFeedback> findByRecording_Id(Long recordingId);

    @Query("""
        SELECT pf.id FROM PeerFeedback pf
        JOIN pf.recording r
        JOIN r.interviewQuestion iq
        JOIN iq.interviewSession s
        WHERE s.user.id = :userId
        AND pf.followUpQuestion IS NOT NULL
        AND pf.followUpQuestion != ''
        AND pf.generatedQuestion IS NULL
    """)
    List<Long> findEligiblePeerFeedbackIdsForUser(@Param("userId") Long userId);
    
    @Query("""
            SELECT pf
            FROM PeerFeedback pf
            JOIN FETCH pf.recording r
            JOIN FETCH r.interviewQuestion iq
            JOIN FETCH iq.interviewSession s
            WHERE pf.user.id = :userId
              AND (:cursor IS NULL OR pf.id < :cursor)
            ORDER BY pf.id DESC
        """)
        List<PeerFeedback> findSliceByUserId(
                @Param("userId") Long userId,
                @Param("cursor") Long cursor,
                Pageable pageable
        );

}
