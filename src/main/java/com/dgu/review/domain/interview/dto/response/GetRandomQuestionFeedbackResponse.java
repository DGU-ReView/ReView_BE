package com.dgu.review.domain.interview.dto.response;

public record GetRandomQuestionFeedbackResponse (
        ProgressStatus progressStatus,
        RandomQuestionFeedbackResult result
){
}
