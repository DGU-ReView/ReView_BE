package com.dgu.review.domain.community.service;

import com.dgu.review.domain.community.dto.*;
import com.dgu.review.domain.community.entity.CommunityPage;
import com.dgu.review.domain.community.entity.DomainCategory;
import com.dgu.review.domain.community.repository.CommunityPageRepository;
import com.dgu.review.domain.user.service.GetUserService;
import com.dgu.review.global.exception.ApiException;
import com.dgu.review.global.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityPageService {

    private final CommunityPageRepository communityRepo;
    private final EntityManager em;
    private final GetUserService getUserService;

    private static final Sort LATEST = Sort.by(Sort.Direction.DESC, "updatedAt");

    // 전체 미리보기 목록
    public List<CategoryPreviewResponse> getAllPreviewsGroupedByDomainWithCursor(Map<String, Long> cursors, int limit) {
        return Arrays.stream(DomainCategory.values())
                .map(category -> {
                    Long cursor = cursors.getOrDefault(category.name(), null);

                    List<CommunityPage> pages = communityRepo.findByCategoryWithCursor(
                            category, cursor, limit
                    );

                    Long nextCursor = pages.isEmpty() ? null : pages.get(pages.size() - 1).getId();

                    List<CommunityPagePreviewResponse> previews = pages.stream()
                            .map(this::toPreviewDto)
                            .toList();

                    return CategoryPreviewResponse.builder()
                            .category(category)
                            .previews(previews)
                            .nextCursor(nextCursor)
                            .build();
                })
                .toList();
    }



    // 키워드 검색
    public List<CategoryPreviewResponse> searchPreviewsGroupedByDomainWithCursor(
            String keyword,
            Map<String, Long> cursors,
            int limit
    ) {
        // 키워드가 없거나 공백이면 빈 리스트 반환
        if (keyword == null || keyword.trim().isEmpty()) {
            return Arrays.stream(DomainCategory.values())
                    .map(category -> CategoryPreviewResponse.builder()
                            .category(category)
                            .previews(List.of())  // 빈 리스트
                            .nextCursor(null)
                            .build())
                    .toList();
        }

        // 키워드 있을 경우
        return Arrays.stream(DomainCategory.values())
                .map(category -> {
                    Long cursor = cursors.getOrDefault(category.name(), null);

                    List<CommunityPage> pages = communityRepo.searchByKeywordAndCategoryWithCursor(
                            keyword, category, cursor, limit
                    );

                    Long nextCursor = pages.isEmpty() ? null : pages.get(pages.size() - 1).getId();

                    List<CommunityPagePreviewResponse> previews = pages.stream()
                            .map(this::toPreviewDto)
                            .toList();

                    return CategoryPreviewResponse.builder()
                            .category(category)
                            .previews(previews)
                            .nextCursor(nextCursor)
                            .build();
                })
                .toList();
    }

    // 상세 조회
    public CommunityPageResponse getDetail(Long pageId) {
        CommunityPage entity = communityRepo.findById(pageId)
                .orElseThrow(() -> new ApiException(ErrorCode.COMMUNITY_PAGE_NOT_FOUND));
        return toDetailDto(entity);
    }

    // 글 생성 (title 자동 생성)
    @Transactional
    public Long create(CommunityPageCreateRequest req) {

        var authorRef = em.getReference(com.dgu.review.domain.user.entity.User.class, getUserService.getUserId());

        String autoTitle = String.format("%s %s %s",
                req.getCompanyName(),
                req.getDomain().getDisplayName(),
                req.getJob());

        CommunityPage entity = CommunityPage.builder()
                .title(autoTitle) // 자동 생성된 제목 저장
                .companyName(req.getCompanyName())
                .domain(req.getDomain())
                .job(req.getJob())
                .interviewPreps(req.getInterviewPreps())
                .answerStrategies(req.getAnswerStrategies())
                .tips(req.getTips())
                .author(authorRef)
                .build();

        CommunityPage saved = communityRepo.save(entity);
        return saved.getId();
    }

    // 글 수정 (회사/분야/직무 제외)
    @Transactional
    public CommunityPageResponse update(Long pageId, CommunityPageUpdateRequest req) {
        CommunityPage entity = communityRepo.findById(pageId)
                .orElseThrow(() -> new ApiException(ErrorCode.COMMUNITY_PAGE_NOT_FOUND));

        entity.updateContents(req.getInterviewPreps(), req.getAnswerStrategies(), req.getTips());
        return toDetailDto(entity);
    }

    public List<DomainDropdownResponse> getDomainDropdowns() {
        return Arrays.stream(DomainCategory.values())
                .map(domain -> new DomainDropdownResponse(domain.name(), domain.getDisplayName()))
                .toList();
    }

    // DTO 변환
    // 미리보기에서는 회사/분야/직무로 된 제목만 표시
    private CommunityPagePreviewResponse toPreviewDto(CommunityPage e) {
        return CommunityPagePreviewResponse.builder()
                .id(e.getId())
                .title(e.getTitle())
                .build();
    }

    private CommunityPageResponse toDetailDto(CommunityPage e) {
        return CommunityPageResponse.builder()
                .id(e.getId())
                .companyName(e.getCompanyName())
                .domain(e.getDomain())
                .job(e.getJob())
                .interviewPreps(e.getInterviewPreps())
                .answerStrategies(e.getAnswerStrategies())
                .tips(e.getTips())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
