package com.dgu.review.domain.interview.controller;


import com.dgu.review.domain.interview.dto.response.StartInterviewResponse;
import com.dgu.review.domain.interview.service.InterviewStartService;
import com.dgu.review.global.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dgu.review.domain.interview.dto.InterviewCreateRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController 
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class InterviewStartController {

	private final InterviewStartService interviewStartService;

	@PostMapping(value = "/interview-sessions")
    public ResponseEntity<ApiResponse<StartInterviewResponse>> startInterview(@Valid @RequestBody InterviewCreateRequest req) {
		return ResponseEntity.ok(ApiResponse.ok(interviewStartService.startInterview(req)));
    }

}
