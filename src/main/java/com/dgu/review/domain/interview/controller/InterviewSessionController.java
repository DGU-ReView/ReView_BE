// 녹음 파일 접수 api
/*
client가 S3에 음성 파일을 올려두고 그 키를 백엔드에 알려주면
DB에 레코드 행을 만들어줌.
 */

package com.dgu.review.domain.interview.controller;

import com.dgu.review.domain.interview.dto.request.RecordingCreateRequest;
import com.dgu.review.domain.interview.dto.response.GetFollowUpQuestionResponse;
import com.dgu.review.domain.interview.dto.response.RecordingCreateResponse;
import com.dgu.review.domain.interview.service.InterviewSessionService;
import com.dgu.review.domain.interview.service.SttFeedbackService;
import com.dgu.review.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
//파일이 어떤 상태인지 추적하고, DB에 그 메타데이터(버킷/경로/상태/길이 등)를 저장, 갱신

@Slf4j
@RestController //api 요청을 받는 컨트롤러
@RequestMapping("/api/interview-sessions")
@RequiredArgsConstructor
@Validated //요청값 검증 가능
public class InterviewSessionController {

    private final InterviewSessionService interviewSessionService;
    private final SttFeedbackService sttFeedbackService;

    @PostMapping("/{sessionId}/questions/{questionId}/recordings")
    public ResponseEntity<ApiResponse<RecordingCreateResponse>> submitRecording(
            @PathVariable Long sessionId,
            @PathVariable Long questionId,
            @Valid @RequestBody RecordingCreateRequest request
    ) {
        RecordingCreateResponse response = interviewSessionService.createAndTranscribe(sessionId, questionId, request);
        log.info("[submitRecording:done] sessionId={}, questionId={} (thread={})",
                sessionId, questionId, Thread.currentThread().getName());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.ok(response));
    }
    /**
     * [녹음 상세 조회]: 일단 주석 처리

     @GetMapping("/{id}")
     public ResponseEntity<RecordingManifestDetailResponse> get(@PathVariable Long id) {
     return ResponseEntity.ok(manifestService.get(id));
     }
     */
}
