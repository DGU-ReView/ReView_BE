package com.dgu.review.domain.interview.dto.response;

public record StartInterviewResponse (
        Long sessionId,
        Long firstQuestionId,
        String firstQuestionText
){
}
