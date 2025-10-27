package com.dgu.review.domain.myarchive.dto;

public record MyInterviewRandomQuestionResponse(
        String question,
        String aiFeedback,
        String selfFeedback,
        String sttText,
        String recordingUrl
) {}

