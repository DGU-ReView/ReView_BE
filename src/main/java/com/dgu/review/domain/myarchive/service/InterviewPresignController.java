package com.dgu.review.domain.myarchive.service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dgu.review.domain.interview.dto.RecordingUploadUrlRequest;
import com.dgu.review.domain.interview.dto.RecordingUploadUrlResponse;
import com.dgu.review.domain.interview.dto.ResumeUploadUrlResponse;
import com.dgu.review.domain.interview.service.InterviewPresignService;





@RestController
@RequiredArgsConstructor
@RequestMapping("/api/presign")
public class InterviewPresignController {

 private final InterviewPresignService interviewUploadService;

 @PostMapping("/recording")
 public ResponseEntity<RecordingUploadUrlResponse> presignRecordPutUrl(
         @Valid @RequestBody RecordingUploadUrlRequest req
 ) {
     // 목 userId
     Long userId = 123L; 

     RecordingUploadUrlResponse res = interviewUploadService.createRecordingPutUrl(req, userId);
     return ResponseEntity.ok(res);
 }
 
 @PostMapping("/resume")
 public ResponseEntity<ResumeUploadUrlResponse> presignResumePutUrl(@RequestParam(name = "fileName", required = true) String fileName) {
     // 목 userId
     Long mocuserId = 123L; 
     ResumeUploadUrlResponse res = interviewUploadService.createResumePutUrl(mocuserId, fileName);
     return ResponseEntity.ok(res);
 }
}