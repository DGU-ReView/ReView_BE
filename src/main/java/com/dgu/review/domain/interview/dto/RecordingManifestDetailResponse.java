package com.dgu.review.domain.interview.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
//사용자가 특정 녹음 파일 상세 정보를 조회할 때.
@Getter @AllArgsConstructor
public class RecordingManifestDetailResponse {
    private Long recordingId;
    private String objectKey;
    private String status;
}
