package com.dgu.review.domain.interview.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

//사용자가 STT 결과/상태를 조회할 때.
@Getter @AllArgsConstructor
public class SttJobDetailResponse {
    private Long recordingId;
    private String status;     // UPLOADED / TRANSCRIBING / SUCCEEDED
    private String sttText;    // 완료 시에만 표시될 결과값
    private String resultSrt;  // 전사된 자막 파일
    private String error;      // 전사 실패했을때 오류 메시지
}


/*
예시 값
{
  "recordingId": 1,
  "status": "FAILED",
  "sttText": null,
  "resultSrt": null,
  "error": "Transcription failed: audio file is corrupted"
}
 */