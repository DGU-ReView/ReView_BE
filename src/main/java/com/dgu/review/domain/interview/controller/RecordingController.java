package com.dgu.review.domain.interview.controller;

import com.dgu.review.domain.interview.dto.request.RecordingCreateRequest;
import com.dgu.review.domain.interview.dto.response.RecordingCreateResponse;
import com.dgu.review.domain.interview.dto.response.RecordingResultsResponse;
import com.dgu.review.domain.interview.service.InterviewSessionService;
import com.dgu.review.domain.interview.service.SttService;
import com.dgu.review.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
public class RecordingController {

    private final InterviewSessionService interviewSessionService;
    private final SttService sttService;

    @PostMapping("/questions/{questionId}/recordings")
    public ResponseEntity<ApiResponse<RecordingCreateResponse>> submitRecording(
            @PathVariable Long questionId,
            @Valid @RequestBody RecordingCreateRequest request
    ) {
        RecordingCreateResponse response = interviewSessionService.createAndTranscribe(questionId, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.ok(response));
    }

    @GetMapping("/recordings/{recordingId}/results")
    public ResponseEntity<ApiResponse<RecordingResultsResponse>> getRecordingStatus(@PathVariable Long recordingId) {
        var res = sttService.getRecordingResults(recordingId);
        return ResponseEntity.ok(ApiResponse.ok(res));
    }

}
