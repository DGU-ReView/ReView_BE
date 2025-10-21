package com.dgu.review.domain.myarchive.dto;


import lombok.Builder;

public record RootQuestionCard(
        int order,
        Long questionId
) { @Builder public RootQuestionCard {} }