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

    private String interviewPreps;
    private String answerStrategies;
    private String tips;
}
