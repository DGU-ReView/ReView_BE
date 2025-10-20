package com.dgu.review.domain.peerfeedback.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PeerFeedbackRequest {

    @NotBlank(message = "평가 내용은 필수입니다.")
    @Size(min = 100, max = 500, message = "평가 내용은 100자 이상 500자 이하로 입력해야 합니다.")
    private String body;

    @Size(max = 30, message = "추가질문은 최대 30자까지 입력 가능합니다.")
    private String followUpQuestion;
}