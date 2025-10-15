package com.dgu.review.domain.peerFeedback.repository;


import com.dgu.review.domain.peerFeedback.entity.PeerFeedback;
import com.dgu.review.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PeerFeedbackRepository extends JpaRepository<PeerFeedback, Long> {

    List<PeerFeedback> findAllByUser(User user);
}
