package com.dgu.review.domain.myarchive.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dgu.review.domain.myarchive.dto.AnswerCheckItem;
import com.dgu.review.domain.myarchive.dto.CursorPageResponse;
import com.dgu.review.domain.myarchive.dto.MyInterviewListItemResponse;
import com.dgu.review.domain.myarchive.dto.MyInterviewSummaryResponse;
import com.dgu.review.domain.myarchive.dto.MyInterviewTitleUpdateRequest;
import com.dgu.review.domain.myarchive.dto.MyInterviewTitleUpdateResponse;
import com.dgu.review.domain.myarchive.service.MyInterviewService;
import com.dgu.review.global.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/myarchive/myinterviews")
public class MyInterviewController {
	private final MyInterviewService myInterviewService;
	// 응답 형식 바꾸기 
	// 나의 면접 리스트 조회
	@GetMapping
	public ResponseEntity<ApiResponse<CursorPageResponse<MyInterviewListItemResponse>>> getinterviewList(
			@RequestParam(name = "cursor", required = false) Long cursor,
			@RequestParam(name = "limit", defaultValue = "12") int limit) {
		CursorPageResponse<MyInterviewListItemResponse> res = myInterviewService.getMyInterviews(cursor, limit);
		return ResponseEntity.ok(ApiResponse.ok(res));
	}
	
	// 나의 면접 상세 조회 - 전체 요약
	@GetMapping("/{interviewId}/summary")
    public ResponseEntity<ApiResponse<MyInterviewSummaryResponse>> getSummary(@PathVariable Long interviewId) {
		MyInterviewSummaryResponse res = myInterviewService.getMyInterviewSummary(interviewId);
	    return ResponseEntity.ok(ApiResponse.ok(res));
    }
	// 나의 면접 상세 조회 - 질문별 답변 조회 
	@GetMapping("/questions/{questionId}/answers")
    public ResponseEntity<ApiResponse<List<AnswerCheckItem>>> getAnswerCheck(@PathVariable Long questionId) {
        List<AnswerCheckItem> res = myInterviewService.getMyInterviewAnswer(questionId);
        return ResponseEntity.ok(ApiResponse.ok(res));
    }
	// 나의 면접 상세 조회 - 질문별 피드백 조회 
	// 나의 면접 상세 조회 - 랜덤 질문 답변 조회 

	// 나의 면접 제목 수정
	@PatchMapping(value = "/{interviewId}/title")
	public MyInterviewTitleUpdateResponse updatemyinterviewTitle(
			@PathVariable("interviewId") Long interviewId,
			@Valid @RequestBody MyInterviewTitleUpdateRequest request) {
		return myInterviewService.updateTitle(interviewId, request);
	}


	// 나의 면접 세션 삭제
}
