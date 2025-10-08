package com.dgu.review.domain.interview.controller;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


import com.dgu.review.domain.interview.dto.InterviewStartRequest;
import com.dgu.review.domain.interview.entity.InterviewMode;
import com.dgu.review.domain.interview.entity.JobDomain;
import com.dgu.review.domain.interview.entity.JobRole;
import com.dgu.review.domain.interview.service.ResumeExtractionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController 
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class InterviewStartController {
	private final ResumeExtractionService resumeExtractionService;
	private final RedisTemplate<String, String> redisTemplate;
	
	
	@PostMapping(value = "/interview-sessions")
    public void extract(@Valid @RequestBody InterviewStartRequest req) {
		//목 userId
		Long userId = 123L; 
		
        // request body로 
		InterviewMode mode = req.mode();
		JobDomain desiredDomain=req.desiredDomain();
		JobRole desiredRole = req.desiredRole();
		
		String resumeId=req.resumeId();
		String resumeObjectKey=redisTemplate.opsForValue().get("presign:resume:" + resumeId);
		
		// 자소서 변환 시작 
		String resumeText = resumeExtractionService.extractText(resumeId, resumeObjectKey);
		
		// db에 인터뷰 섹션 저장 
		
		
    }

}
