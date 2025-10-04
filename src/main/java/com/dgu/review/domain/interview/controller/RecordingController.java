package com.dgu.review.domain.interview.controller;

import com.dgu.review.domain.interview.dto.request.RecordingCreateRequest;
import com.dgu.review.domain.interview.dto.response.RecordingCreateResponse;
import com.dgu.review.domain.interview.dto.response.RecordingResultsResponse;
import com.dgu.review.domain.interview.service.RecordingCommandService;
import com.dgu.review.domain.interview.service.RecordingQueryService;
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

    private final RecordingCommandService recordingCommandService;
    private final SttService sttService;
    private final RecordingQueryService recordingQueryService;

    @PostMapping("/questions/{questionId}/recordings")
    public ResponseEntity<ApiResponse<RecordingCreateResponse>> submitRecording(
            @PathVariable Long questionId,
            @Valid @RequestBody RecordingCreateRequest request
    ) {
        RecordingCreateResponse response = recordingCommandService.createAndTranscribe(questionId, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.ok(response));
    }

    @GetMapping("/recordings/{recordingId}/results")
    public ResponseEntity<ApiResponse<RecordingResultsResponse>> getRecordingStatus(@PathVariable Long recordingId) {
        var res = recordingQueryService.getRecordingResults(recordingId);
        return ResponseEntity.ok(ApiResponse.ok(res));
    }

}
