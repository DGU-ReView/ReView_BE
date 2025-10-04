package com.dgu.review.domain.interview.controller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dgu.review.domain.interview.service.InterviewGetUrlService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/test")
public class ResumeController {

    private final InterviewGetUrlService interviewGetUrlService;

    @GetMapping("/resume-url")
    public String resumeUrl(
            @RequestParam("userId") long userId,
            @RequestParam("resume") String resumeId,
            @RequestParam("ext") String ext
    ) {
        String url = interviewGetUrlService.createResumeGetUrl(userId, resumeId, ext);
        return url;
    }
}