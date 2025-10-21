package com.dgu.review.domain.interview.controller;

import com.dgu.review.domain.interview.dto.response.GetInterviewResultsResponse;
import com.dgu.review.domain.interview.service.InterviewService;
import com.dgu.review.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/interview-sessions")
public class InterviewController {

    private final InterviewService interviewService;

    @GetMapping("/{sessionId}")
    public ResponseEntity<ApiResponse<GetInterviewResultsResponse>> getInterviewResults(
            @PathVariable Long sessionId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(interviewService.getInterviewResults(sessionId)));
    }

}
