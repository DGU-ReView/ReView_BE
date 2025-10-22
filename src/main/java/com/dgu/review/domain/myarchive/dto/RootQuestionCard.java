package com.dgu.review.domain.myarchive.dto;


import lombok.Builder;

@Builder
public record RootQuestionCard(
        int order,
        Long questionId
) { }