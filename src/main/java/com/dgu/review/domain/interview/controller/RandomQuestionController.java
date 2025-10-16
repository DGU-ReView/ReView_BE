package com.dgu.review.domain.interview.controller;

import com.dgu.review.domain.interview.dto.response.GetRandomQuestionFeedbackResponse;
import com.dgu.review.domain.interview.dto.response.GetRandomQuestionResponse;
import com.dgu.review.domain.interview.service.RandomQuestionService;
import com.dgu.review.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/random-questions/peer")
@RequiredArgsConstructor
public class RandomQuestionController {

    private final RandomQuestionService randomQuestionService;

    @GetMapping("/{peerAnswerId}")
    public ResponseEntity<ApiResponse<GetRandomQuestionResponse>> getRandomQuestion(
            @PathVariable Long peerAnswerId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(randomQuestionService.getRandomQuestion(peerAnswerId)));
    }

    @GetMapping("/{recordingId}/feedbacks")
    public ResponseEntity<ApiResponse<GetRandomQuestionFeedbackResponse>> getRandomQuestionFeedback(
            @PathVariable Long recordingId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(randomQuestionService.getRandomQuestionFeedback(recordingId)));
    }


}
