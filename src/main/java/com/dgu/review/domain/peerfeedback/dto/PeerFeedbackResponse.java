package com.dgu.review.domain.peerFeedback.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PeerFeedbackResponse {
    private Long feedbackId;          // 피드백 ID
    private String question;          // 질문 내용
    private String body;              // 평가 본문
    private String followUpQuestion;  // 추가 질문
    private String jobRole;           // 피평가자 직무명
    private LocalDateTime createdAt;  // 작성 시각
}