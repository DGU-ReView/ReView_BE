package com.dgu.review.domain.interview.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record QuestionSummary (
        int questionNumber,
        String rootQuestion,
        String aiFeedback,
        String selfFeedback,
        List<QnATurn> qnaTurns
){
}
