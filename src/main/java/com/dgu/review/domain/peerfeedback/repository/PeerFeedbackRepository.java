package com.dgu.review.domain.peerFeedback.repository;


import com.dgu.review.domain.peerFeedback.entity.PeerFeedback;
import com.dgu.review.domain.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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
}
