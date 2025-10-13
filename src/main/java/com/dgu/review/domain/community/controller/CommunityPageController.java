package com.dgu.review.domain.community.controller;

import com.dgu.review.domain.community.dto.*;
import com.dgu.review.domain.community.entity.DomainCategory;
import com.dgu.review.domain.community.service.CommunityPageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/community/pages")
@RequiredArgsConstructor
public class CommunityPageController {

    private final CommunityPageService communityService;

    /**
     * [GET] 전체 목록 미리보기
     * 예: GET /api/community/pages?limit=12
     */
    @GetMapping
    public ResponseEntity<List<CategoryPreviewResponse>> getAll(
            @RequestParam Map<String, Long> cursors,  // ex: ?IT_ENGINEERING=123&BUSINESS_FINANCE=50
            @RequestParam(defaultValue = "5") int limit
    ) {
        var previews = communityService.getAllPreviewsGroupedByDomainWithCursor(cursors, limit);
        return ResponseEntity.ok(previews);
    }

    /**
     * [GET] 키워드 검색(회사/직무/분야)
     * 예: GET /api/community/pages/search?q=백엔드&limit=6
     */
    @GetMapping("/search")
    public ResponseEntity<List<CommunityPagePreviewResponse>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int limit
    ) {
        var previews = communityService.searchPreviews(q, limit);
        return ResponseEntity.ok(previews);
    }


    /**
     * [GET] 상세 조회
     * 예: GET /api/community/pages/123
     */
    @GetMapping("/{pageId}")
    public ResponseEntity<CommunityPageResponse> getOne(@PathVariable Long pageId) {
        var detail = communityService.getDetail(pageId);
        return ResponseEntity.ok(detail);
    }

    /**
     * [POST] 새 글 작성
     * POST /api/community/pages
     */
    @PostMapping
    public ResponseEntity<Void> create(@Valid @RequestBody CommunityPageCreateRequest req) {
        Long newId = communityService.create(req);         // 저장 후 PK 수령
        URI location = URI.create("/api/community/pages/" + newId); // 상세 URL
        return ResponseEntity.created(location).build();   // 201 + Location 헤더
    }

    /**
     * [PATCH] 글 수정(회사/분야/직무 제외)
     * 예: PATCH /api/community/pages/123
     */
    @PatchMapping("/{pageId}")
    public ResponseEntity<CommunityPageResponse> update(
            @PathVariable Long pageId,
            @Valid @RequestBody CommunityPageUpdateRequest req
    ) {
        var updated = communityService.update(pageId, req); // 수정 후 상세 DTO 반환
        return ResponseEntity.ok(updated);                  // 200 OK + 수정된 본문
    }

    /**
     * [GET] 드롭다운 분야 목록
     * 새 글 작성 시 사용자가 선택할 분야 목록을 제공하는 API
     * GET /api/community/pages/dropdown
     */
    @GetMapping("/dropdown")
    public ResponseEntity<List<DomainDropdownResponse>> dropdown() {
        return ResponseEntity.ok(communityService.getDomainDropdowns());
    }
}
