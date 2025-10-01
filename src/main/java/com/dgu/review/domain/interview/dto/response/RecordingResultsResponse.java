package com.dgu.review.domain.interview.dto.response;

import com.dgu.review.domain.interview.entity.RecordingStatus;

public record RecordingResultsResponse (
        Long recordingId,
        RecordingStatus status,
        String sttText,
        String followUpQuestion
){
}
