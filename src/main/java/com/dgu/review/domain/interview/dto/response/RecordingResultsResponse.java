package com.dgu.review.domain.interview.dto.response;

import lombok.Builder;

@Builder
public record RecordingResultsResponse (
        Long sessionId, // 질문이 모두 끝났을 때 피드백 요청용
        ProgressStatus status,
        NextPayload next
){
}
