package com.dgu.review.domain.myarchive.dto;


public record MyPeerFeedbackResponse(
		String jobRole,
		String createdAt,
		String question,
		String myfeedback,
		String myfollowUpQuestion
) {}



