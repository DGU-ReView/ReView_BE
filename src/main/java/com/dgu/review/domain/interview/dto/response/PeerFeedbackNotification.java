package com.dgu.review.domain.interview.dto.response;

public record PeerFeedbackNotification (
        String jobName,
        String interviewName,
        int questionNumber,
        Long peerFeedbackId
){
}
