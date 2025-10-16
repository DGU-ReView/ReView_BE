package com.dgu.review.domain.peerFeedback.controller;

import com.dgu.review.domain.peerFeedback.dto.PeerFeedbackRequest;
import com.dgu.review.domain.peerFeedback.dto.PeerFeedbackResponse;
import com.dgu.review.domain.peerFeedback.dto.RandomRecordingResponse;
import com.dgu.review.domain.peerFeedback.service.PeerFeedbackService;
import com.dgu.review.domain.user.entity.User;
import com.dgu.review.domain.user.repository.UserRepository;
import com.dgu.review.global.exception.ApiException;
import com.dgu.review.global.exception.ErrorCode;
import com.dgu.review.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/peer-reviews")
public class PeerFeedbackController {

    private final PeerFeedbackService peerFeedbackService;
    private final UserRepository userRepository;
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
    public ResponseEntity<Void> createFeedback(
            @PathVariable Long recordingId,
            @Valid @RequestBody PeerFeedbackRequest request
    ) {
        // 로그인 미구현 상태이므로 evaluator 하드코딩
        User evaluator = userRepository.findById(1L)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        peerFeedbackService.createFeedback(recordingId, request, evaluator);
        return ResponseEntity.noContent().build();
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
