package com.dgu.review.domain.peerfeedback.service;

import com.dgu.review.domain.interview.entity.InterviewQuestion;
import com.dgu.review.domain.interview.entity.InterviewSession;
import com.dgu.review.domain.interview.entity.Recording;
import com.dgu.review.domain.peerfeedback.dto.PeerFeedbackRequest;
import com.dgu.review.domain.peerfeedback.dto.RandomRecordingResponse;
import com.dgu.review.domain.peerfeedback.entity.PeerFeedback;
import com.dgu.review.domain.peerfeedback.repository.PeerFeedbackRepository;
import com.dgu.review.domain.interview.repository.RecordingRepository;
import com.dgu.review.domain.user.entity.User;
import com.dgu.review.domain.user.service.GetUserService;
import com.dgu.review.global.exception.ApiException;
import com.dgu.review.global.exception.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import com.dgu.review.domain.interview.service.InterviewObjectReadService;

import java.util.Random;


@Service
@RequiredArgsConstructor
@Transactional
public class PeerFeedbackService {

    private final PeerFeedbackRepository peerFeedbackRepository;
    private final RecordingRepository recordingRepository;
    private final InterviewObjectReadService interviewObjectReadService;

    /**
     * 랜덤 녹음 조회
     */
    public RandomRecordingResponse getRandomRecording(Long currentUserId) {

        long totalCount = recordingRepository.countRootRecordingsExcludingUser(currentUserId);
        if (totalCount == 0) {
            return new RandomRecordingResponse(
                    null,      // recordingId
                    null,                // question
                    null,                // sttText
                    null,                // jobRole
                    "현재 평가 가능한 녹음이 없습니다." // recordingUrl 대신 메시지 전달
            );
        }

        int randomIndex = new Random().nextInt((int) totalCount);
        var pageable = PageRequest.of(randomIndex, 1);

        Recording randomRecording = recordingRepository
                .findRootRecordingExcludingUserAndAlreadyEvaluated(currentUserId, pageable)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ApiException(ErrorCode.RECORDING_NOT_FOUND));

        InterviewQuestion question = randomRecording.getInterviewQuestion();
        InterviewSession session = question.getInterviewSession();

        String recordingUrl = interviewObjectReadService.createRecordingGetUrl(randomRecording.getObjectKey());

        return new RandomRecordingResponse(
                randomRecording.getId(),
                question.getQuestion(),
                randomRecording.getSttText(),
                session.getJobRole(),
                recordingUrl
        );
    }

    /**
     * 타인평가 저장
     */
    @Transactional
    public void createFeedback(Long recordingId, PeerFeedbackRequest request, User evaluator) {
        Recording recording = recordingRepository.findById(recordingId)
                .orElseThrow(() -> new ApiException(ErrorCode.RECORDING_NOT_FOUND));

        if (recording.getInterviewQuestion().getInterviewSession().getUser().getId()
                .equals(evaluator.getId())) {
            throw new ApiException(ErrorCode.SELF_FEEDBACK_NOT_ALLOWED);
        }
        //이미 평가한 타인의 녹음 평가 방지.
        if (peerFeedbackRepository.existsByUserAndRecording(evaluator, recording)) {
            throw new ApiException(ErrorCode.DUPLICATE_PEER_FEEDBACK);
        }

        PeerFeedback feedback = PeerFeedback.builder()
                .recording(recording)
                .user(evaluator)
                .body(request.getBody())
                .followUpQuestion(request.getFollowUpQuestion())
                .build();

        peerFeedbackRepository.save(feedback);
    }
    private final GetUserService getUserService;
    
}
