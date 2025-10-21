package com.dgu.review.domain.interview.controller;

import com.dgu.review.domain.interview.dto.response.GetRandomQuestionFeedbackResponse;
import com.dgu.review.domain.interview.dto.response.GetRandomQuestionResponse;
import com.dgu.review.domain.interview.dto.response.RecordingCreateResponse;
import com.dgu.review.domain.interview.service.RandomQuestionService;
import com.dgu.review.domain.interview.service.RecordingCommandService;
import com.dgu.review.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/random-questions/peer")
@RequiredArgsConstructor
public class RandomQuestionController {

    private final RandomQuestionService randomQuestionService;
    private final RecordingCommandService recordingCommandService;

    @GetMapping("/{peerAnswerId}")
    public ResponseEntity<ApiResponse<GetRandomQuestionResponse>> getRandomQuestion(
            @PathVariable Long peerAnswerId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(randomQuestionService.getRandomQuestion(peerAnswerId)));
    }

    // question에 대한 피드백 조회 로직으로 변경 고려 | 이걸 폴링으로 사용
    @GetMapping("/recordings/{recordingId}/feedbacks")
    public ResponseEntity<ApiResponse<GetRandomQuestionFeedbackResponse>> getRandomQuestionFeedback(
            @PathVariable Long recordingId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(randomQuestionService.getRandomQuestionFeedback(recordingId)));
    }

    @PostMapping("/questions/{questionId}")
    public ResponseEntity<ApiResponse<RecordingCreateResponse>> createRandomQuestionRecording(
            @PathVariable Long questionId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(recordingCommandService.createAndTranscribeForRandom(questionId)));
    }


}
