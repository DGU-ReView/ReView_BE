package com.dgu.review.domain.myarchive.dto;


import lombok.Builder;

public record AnswerCheckItem(
        int order,
        Long questionId,
        String question,
        String answerText,
        String recordUrl
) { @Builder public AnswerCheckItem {} }
