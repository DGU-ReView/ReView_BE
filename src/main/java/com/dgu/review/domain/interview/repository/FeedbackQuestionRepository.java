package com.dgu.review.domain.interview.repository;

import com.dgu.review.domain.interview.entity.FeedbackQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackQuestionRepository extends JpaRepository<FeedbackQuestion, Long> {

}
