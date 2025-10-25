package com.dgu.review.domain.interview.dto.response;

import lombok.Builder;

public record GetRandomQuestionResponse(
    Question question,
    Context context
) {
    public record Question(
            Long questionId,
            String questionText
    ) {}

    public record Context(
            Long questionId,
            String questionText,
            String presignedRecordingGetUrl,
            String sttText
    ) {}
}
