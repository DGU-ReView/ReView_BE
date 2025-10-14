package com.dgu.review.domain.interview.controller;


import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dgu.review.domain.interview.dto.InterviewCreateRequest;
import com.dgu.review.domain.interview.service.InterviewPreparationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController 
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class InterviewStartController {
	private final InterviewPreparationService interviewPreparationService;

	
	@PostMapping(value = "/interview-sessions")
    public void extract(@Valid @RequestBody InterviewCreateRequest req) {

		// 자소서 변환 & db 저장 
		String resumeText = interviewPreparationService.extractText(req);
		log.info("자소서 텍스트:{}",resumeText);
		
    }

}
