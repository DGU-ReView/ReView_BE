package com.dgu.review.domain.interview.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

public record RecordingCreateResponse (
    Long recordingId,
    String status
){
}
