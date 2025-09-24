package com.dgu.review.domain.interview.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
//녹음이 성공적으로 등록된 후, 서버가 클라이언트에 응답할 때 사용.

@Getter @AllArgsConstructor
public class RecordingCreateResponse {
    private Long recordingId; //녹음의 고유 ID
    private String status; // 현재 상태(UPLOADED)
}
