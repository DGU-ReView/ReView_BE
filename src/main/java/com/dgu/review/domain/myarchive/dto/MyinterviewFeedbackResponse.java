package com.dgu.review.domain.myarchive.dto;

import java.util.List;

import lombok.Builder;

public record MyinterviewFeedbackResponse(
        String aiFeedback,
        String selfFeedback,
        List<String> peerFeedbacks 
) {}