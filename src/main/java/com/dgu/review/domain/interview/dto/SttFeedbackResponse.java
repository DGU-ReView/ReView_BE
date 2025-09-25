package com.dgu.review.domain.interview.dto;

import lombok.Builder;

@Builder
public record SttFeedbackResponse (
    String feedback,
    String modelId,
    int promptTokens,
    int completionTokens,
    String stopReason,
    boolean truncated
){
}
