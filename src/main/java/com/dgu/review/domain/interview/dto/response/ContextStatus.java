package com.dgu.review.domain.interview.dto.response;

public record ContextStatus(
        Long sessionId,
        boolean sessionCompleted,
        int currentRootNumber
){
}
