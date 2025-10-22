package com.dgu.review.domain.interview.dto.response;

import lombok.Builder;

@Builder
public record RandomQuestionFeedbackResult(
        Long questionId,
        String questionText,
        String aiFeedback,
        String selfFeedback,
        String presignedRecordingGetUrl,
        String sttText
){
}
