package com.dgu.review.domain.interview.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

//STT 작업 등록이 성공적으로 접수된 후, 서버가 클라이언트에 응답.
@Getter @AllArgsConstructor
public class SttEnqueueResponse {

    private String status; // 전사 시작 직후이므로 TRANSCRIBING
}
