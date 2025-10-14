package com.dgu.review.domain.interview.dto.response;

import lombok.Builder;

@Builder
public record GetFollowUpQuestionResponse(
        String followUpQuestion
){
}
