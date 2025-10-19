package com.dgu.review.domain.interview.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record GetInterviewResultsResponse (
        ProgressStatus feedbackProgressStatus,
        InterviewSummary interviewSummary
){
}
