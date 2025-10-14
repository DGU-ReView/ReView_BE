package com.dgu.review.domain.interview.dto.response;

import lombok.Builder;

@Builder
public record NextPayload (
        NextQuestionType type,
        Long nextQuestionId,
        String nextQuestionText,
        Long rootId,
        String rootText,
        int rootIndex
){
}
