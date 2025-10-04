package com.dgu.review.domain.interview.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dgu.review.domain.interview.dto.PresignRecordingUploadRequest;
import com.dgu.review.domain.interview.dto.PresignRecordingUploadResponse;
import com.dgu.review.domain.interview.dto.PresignResumeUploadResponse;
import com.dgu.review.domain.interview.service.InterviewUploadService;

import io.swagger.v3.oas.annotations.parameters.RequestBody;



@RestController
@RequiredArgsConstructor
@RequestMapping("/api/uploads")
public class InterviewUploadController {

 private final InterviewUploadService interviewUploadService;

 @PostMapping("/recording")
 public ResponseEntity<PresignRecordingUploadResponse> presignRecordPutUrl(
         @Valid @RequestBody PresignRecordingUploadRequest req
 ) {
     // 목 userId
     Long userId = 123L; 

     PresignRecordingUploadResponse res = interviewUploadService.createRecordingPutUrl(req, userId);
     return ResponseEntity.ok(res);
 }
 
 @PostMapping("/resume")
 public ResponseEntity<PresignResumeUploadResponse> presignResumePutUrl(@RequestParam(name = "fileName", required = true) String fileName) {
     // 목 userId
     Long mocuserId = 123L; 
     PresignResumeUploadResponse res = interviewUploadService.createResumePutUrl(mocuserId, fileName);
     return ResponseEntity.ok(res);
 }
}
