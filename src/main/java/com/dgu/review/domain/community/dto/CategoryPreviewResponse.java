package com.dgu.review.domain.community.dto;

import com.dgu.review.domain.community.entity.DomainCategory;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CategoryPreviewResponse {
    private DomainCategory category;
    private List<CommunityPagePreviewResponse> previews;
    private Long nextCursor;

}