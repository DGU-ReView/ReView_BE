package com.dgu.review.domain.myarchive.dto;

public record MyPeerFeedbackCardResponse(
        String title,
        String question,
        String body
) {}