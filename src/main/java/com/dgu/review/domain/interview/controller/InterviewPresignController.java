
package com.dgu.review.domain.interview.controller;

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
import com.dgu.review.domain.user.service.GetUserService;





@RestController
@RequiredArgsConstructor
@RequestMapping("/api/presign")
public class InterviewPresignController {

 private final InterviewPresignService interviewUploadService;
 private final GetUserService getUserService;

 @PostMapping("/recording")
 public ResponseEntity<RecordingUploadUrlResponse> presignRecordPutUrl(
         @Valid @RequestBody RecordingUploadUrlRequest req
 ) {
     Long userId = getUserService.getUserId(); 

     RecordingUploadUrlResponse res = interviewUploadService.createRecordingPutUrl(req, userId);
     return ResponseEntity.ok(res);
 }
 
 @PostMapping("/resume")
 public ResponseEntity<ResumeUploadUrlResponse> presignResumePutUrl(@RequestParam(name = "fileName", required = true) String fileName) {
	 Long userId = getUserService.getUserId(); 
     ResumeUploadUrlResponse res = interviewUploadService.createResumePutUrl(userId, fileName);
     return ResponseEntity.ok(res);
 }
}
