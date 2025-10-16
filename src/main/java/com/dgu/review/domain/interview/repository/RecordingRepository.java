package com.dgu.review.domain.interview.repository;


import com.dgu.review.domain.interview.entity.Recording;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import java.util.List;


public interface RecordingRepository extends JpaRepository<Recording, Long> {

    // 전체 루트 녹음 갯수
    @Query("""
        SELECT COUNT(r)
        FROM Recording r
        JOIN r.interviewQuestion iq
        JOIN iq.interviewSession s
        WHERE iq.parentQuestion IS NULL
          AND s.user.id <> :currentUserId
    """)
    long countRootRecordingsExcludingUser(@Param("currentUserId") Long currentUserId);

    // 특정 offset에서 1개만 조회
    @Query("""
        SELECT r
        FROM Recording r
        JOIN FETCH r.interviewQuestion iq
        JOIN FETCH iq.interviewSession s
        WHERE iq.parentQuestion IS NULL
          AND s.user.id <> :currentUserId
    """)
    List<Recording> findRootRecordingAtOffset(@Param("currentUserId") Long currentUserId, Pageable pageable);
}