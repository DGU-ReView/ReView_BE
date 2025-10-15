package com.dgu.review.domain.peerfeedback.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PeerFeedbackResponse {
    private Long feedbackId;
    private Long recordingId;
    private Long evaluatorId;
    private Long intervieweeId;
    private String body;
    private String followUpQuestion;
    private LocalDateTime createdAt;
}