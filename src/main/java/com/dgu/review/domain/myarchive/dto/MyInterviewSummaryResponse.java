package com.dgu.review.domain.myarchive.dto;

import java.util.List;

import lombok.Builder;


public record MyInterviewSummaryResponse(
        String title,
        int timedOutCount,
        List<RootQuestionCard> questionCards,   
        List<AnswerCheckItem> firstQuestionThread
) { @Builder public MyInterviewSummaryResponse {} }
