package com.dgu.review.domain.interview.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dgu.review.domain.interview.dto.PresignRecordUploadRequest;
import com.dgu.review.domain.interview.dto.PresignUploadResponse;
import com.dgu.review.domain.interview.service.InterviewUploadService;

import io.swagger.v3.oas.annotations.parameters.RequestBody;



@RestController
@RequiredArgsConstructor
@RequestMapping("/api/uploads")
public class InterviewUploadController {

 private final InterviewUploadService service;

 @PostMapping("/recording")
 public ResponseEntity<PresignUploadResponse> presignRecordPutUrl(
         @Valid @RequestBody PresignRecordUploadRequest req
 ) {
     // 목 userId
     Long userId = 123L; 

     PresignUploadResponse res = service.createRecordPutUrl(req, userId);
     return ResponseEntity.ok(res);
 }
 
 @PostMapping("/resume")
 public ResponseEntity<PresignUploadResponse> presignResumePutUrl(@RequestParam String fileName) {
     // 목 userId
     Long mocuserId = 123L; 
     PresignUploadResponse res = service.createResumePutUrl(mocuserId, fileName);
     return ResponseEntity.ok(res);
 }
}
