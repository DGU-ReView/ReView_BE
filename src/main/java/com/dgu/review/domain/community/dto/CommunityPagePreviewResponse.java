package com.dgu.review.domain.community.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 커뮤니티 미리보기 DTO
 * title만 표시.
 */
@Getter
@Builder
public class CommunityPagePreviewResponse {
    private Long id;       // 게시글 ID
    private String title;  // "회사 + 분야 + 직무" 자동 생성 제목
}
