package com.dgu.review.domain.peerfeedback.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RandomRecordingResponse {

    private Long recordingId;       // 녹음 ID
    private String question;        // 질문 내용
    private String sttText;         // 답변 텍스트(STT)
    private String jobRole;         // 직군
    private String recordingUrl;    // S3 Presigned GET URL
    private String message;         // 추가 메세지
}