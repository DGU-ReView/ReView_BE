package com.dgu.review.domain.myarchive.dto;


import lombok.Builder;

@Builder
public record AnswerCheckItem(
        int order,
        Long questionId,
        String question,
        String answerText,
        String recordUrl
) { }
