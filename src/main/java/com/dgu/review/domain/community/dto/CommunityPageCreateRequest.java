package com.dgu.review.domain.community.dto;

import com.dgu.review.domain.community.entity.DomainCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 새 글 등록 시 클라이언트 -> 서버로 들어오는 요청 바디.
 */

@Getter
@Setter
@NoArgsConstructor
public class CommunityPageCreateRequest {

    @NotBlank(message = "회사명은 필수입니다.")
    private String companyName;       // 회사명

    @NotNull(message = "분야는 필수입니다.")
    private DomainCategory domain;    // 분야

    @NotBlank(message = "직무는 필수입니다.")
    private String job;               // 직무

    @NotBlank(message = "면접 대비 내용은 필수입니다.")
    private String interviewPreps;    // 면접 대비

    @NotBlank(message = "답변 전략은 필수입니다.")
    private String answerStrategies;  // 답변 전략

    @NotBlank(message = "팁/기타는 필수입니다.")
    private String tips;              // 기타/팁
}
