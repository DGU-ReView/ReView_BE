package com.dgu.review.domain.interview.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record GetInterviewResultsResponse (
        String interviewTitle,
        int timeoutQuestionNumber,
        List<QuestionSummary> questionSummaries
){
}
