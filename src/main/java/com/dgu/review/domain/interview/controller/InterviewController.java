package com.dgu.review.domain.interview.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.dgu.review.domain.interview.service.TextExtractService;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController 
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class InterviewController {
	private final TextExtractService service;
	
	
	@PostMapping(value = "/interview-sessions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void extract(@RequestParam("자기소개서") MultipartFile file) throws Exception {
        String text = service.extractText(file.getInputStream());
        log.info("자소서 내용 : {} ",text );
    }

}
