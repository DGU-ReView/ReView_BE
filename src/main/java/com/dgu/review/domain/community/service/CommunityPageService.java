package com.dgu.review.domain.community.service;

import com.dgu.review.domain.community.dto.*;
import com.dgu.review.domain.community.entity.CommunityPage;
import com.dgu.review.domain.community.entity.DomainCategory;
import com.dgu.review.domain.community.repository.CommunityPageRepository;
import com.dgu.review.global.exception.ApiException;
import com.dgu.review.global.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityPageService {

    private final CommunityPageRepository communityRepo;
    private final EntityManager em;

    private static final Sort LATEST = Sort.by(Sort.Direction.DESC, "createdAt");

    // 전체 미리보기 목록
    public List<CommunityPagePreviewResponse> getAllPreviews(int limit) {
        Pageable pageable = PageRequest.of(0, limit, LATEST);
        Page<CommunityPage> page = communityRepo.findAllByOrderByCreatedAtDesc(pageable);
        return page.getContent().stream()
                .map(this::toPreviewDto)
                .collect(Collectors.toList());
    }


    // 키워드 검색
    public List<CommunityPagePreviewResponse> searchPreviews(String keyword, int limit) {
        Pageable pageable = PageRequest.of(0, limit, LATEST);
        Page<CommunityPage> page = communityRepo.searchByKeyword(keyword, pageable);
        return page.getContent().stream().map(this::toPreviewDto).collect(Collectors.toList());
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
        //로그인 사용자 정보로 대체 예정. 현재는 테스트용 id=1로 적어둠.
        var authorRef = em.getReference(com.dgu.review.domain.user.entity.User.class, 1L);

        String autoTitle = String.format("%s %s %s",
                req.getCompanyName(),
                req.getDomain(),
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

    // 글 삭제
    @Transactional
    public void delete(Long pageId) {
        if (!communityRepo.existsById(pageId)) {
            throw new ApiException(ErrorCode.COMMUNITY_PAGE_NOT_FOUND);
        }
        communityRepo.deleteById(pageId);
    }

    public List<String> getDomains() {
        return Arrays.stream(DomainCategory.values())
                .map(DomainCategory::getDisplayName)
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
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
