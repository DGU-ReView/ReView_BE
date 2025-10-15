package com.dgu.review.domain.peerfeedback.controller;

import com.dgu.review.domain.peerfeedback.dto.PeerFeedbackRequest;
import com.dgu.review.domain.peerfeedback.dto.PeerFeedbackResponse;
import com.dgu.review.domain.peerfeedback.dto.RandomRecordingResponse;
import com.dgu.review.domain.peerfeedback.service.PeerFeedbackService;
import com.dgu.review.domain.user.entity.User;
import com.dgu.review.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/peer-reviews")
public class PeerFeedbackController {

    private final PeerFeedbackService peerFeedbackService;

    /**
     * 랜덤 평가 대상 조회
     */
    @GetMapping("/random")
    public ResponseEntity<ApiResponse<RandomRecordingResponse>> getRandomRecording() {

        // 임시
        Long currentUserId = 1L; // 추후 교체

        var res = peerFeedbackService.getRandomRecording(currentUserId);
        return ResponseEntity.ok(ApiResponse.ok(res));
    }

    /**
     * 타인평가 제출
     */
    @PostMapping("/recordings/{recordingId}/feedbacks")
    public ResponseEntity<ApiResponse<PeerFeedbackResponse>> submitFeedback(
            @PathVariable Long recordingId,
            @Validated @RequestBody PeerFeedbackRequest request
    ) {
        // 임시
        User evaluator = User.builder()
                .id(1L) // 로그인 완성되면 SecurityContext에서 대체
                .email("testuser@example.com")
                .build();

        var res = peerFeedbackService.createFeedback(recordingId, request, evaluator);
        return ResponseEntity.ok(ApiResponse.ok(res));
    }

    /**
     * 내가 작성한 평가 상세 조회
     */
    @GetMapping("/mypage/feedbacks/{feedbackId}")
    public ResponseEntity<ApiResponse<PeerFeedbackResponse>> getMyFeedbackDetail(
            @PathVariable Long feedbackId
    ) {
        var res = peerFeedbackService.getFeedbackDetail(feedbackId);
        return ResponseEntity.ok(ApiResponse.ok(res));
    }
}
