package com.dgu.review.domain.interview.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter @AllArgsConstructor
public class RecordingCreateResponse {
    private Long recordingId;
    private String status;
}
