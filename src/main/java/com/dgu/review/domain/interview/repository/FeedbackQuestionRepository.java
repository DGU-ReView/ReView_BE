package com.dgu.review.domain.interview.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.dgu.review.domain.interview.entity.FeedbackQuestion;

public interface FeedbackQuestionRepository extends JpaRepository<FeedbackQuestion, Long> {

}
