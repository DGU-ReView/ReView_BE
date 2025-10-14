package com.dgu.review.domain.interview.dto.response;

import lombok.Builder;

@Builder
public record QuestionSummary (
        int questionNumber,
        String rootQuestion,
        String sttText,
        String aiFeedback,
        String selfFeedback
){
}
