package com.dgu.review.domain.myarchive.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dgu.review.domain.myarchive.dto.CursorPageResponse;
import com.dgu.review.domain.myarchive.dto.MyFeedbackListItemResponse;
import com.dgu.review.domain.myarchive.service.MyfeedbackService;
import com.dgu.review.global.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/myarhive/myfeedbacks")
public class MyfeedbackController {

    private final MyfeedbackService myFeedbackService;

    // 내가 한 피드백 리스트 조회
    @GetMapping
    public ResponseEntity<ApiResponse<CursorPageResponse<MyFeedbackListItemResponse>>> getMyFeedbackList(
            @RequestParam(name = "cursor", required = false) Long cursor,
            @RequestParam(name = "limit", defaultValue = "12") int limit
    ) {
        CursorPageResponse<MyFeedbackListItemResponse> res =
                myFeedbackService.getMyFeedbacks(cursor, limit);

        return ResponseEntity.ok(ApiResponse.ok(res));
    }
    
    // 내가 한 피드백 상세 조회 
}