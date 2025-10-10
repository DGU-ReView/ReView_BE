package com.dgu.review.domain.community.dto;

import com.dgu.review.domain.community.entity.DomainCategory;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 상세 조회 응답(모든 주요 필드 포함).
 */
@Getter
@Builder
public class CommunityPageResponse {
    private Long id;
    private String companyName;
    private DomainCategory domain;
    private String job;
    private String interviewPreps;
    private String answerStrategies;
    private String tips;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
