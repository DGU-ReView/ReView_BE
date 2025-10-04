package com.dgu.review.domain.interview.controller;

import com.dgu.review.domain.interview.dto.response.RecordingResultsResponse;
import com.dgu.review.domain.interview.service.SttService;
import com.dgu.review.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/interview-sessions")
@RequiredArgsConstructor
@Validated
public class SttController {

    private final SttService sttService;



}