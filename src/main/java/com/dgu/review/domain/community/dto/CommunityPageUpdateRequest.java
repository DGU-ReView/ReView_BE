package com.dgu.review.domain.community.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 수정 가능 필드만 포함(회사/분야/직무 제외).
 */
@Getter
@Setter
@NoArgsConstructor
public class CommunityPageUpdateRequest {

    @NotBlank(message = "면접 대비 내용은 필수입니다.")
    private String interviewPreps;

    @NotBlank(message = "답변 전략은 필수입니다.")
    private String answerStrategies;

    @NotBlank(message = "팁/기타는 필수입니다.")
    private String tips;
}
