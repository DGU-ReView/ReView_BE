package com.dgu.review.domain.peerfeedback.service;

import com.dgu.review.domain.interview.entity.InterviewQuestion;
import com.dgu.review.domain.interview.entity.InterviewSession;
import com.dgu.review.domain.interview.entity.Recording;
import com.dgu.review.domain.peerfeedback.dto.PeerFeedbackRequest;
import com.dgu.review.domain.peerfeedback.dto.PeerFeedbackResponse;
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
            throw new ApiException(ErrorCode.RECORDING_NOT_FOUND);
        }

        int randomIndex = new Random().nextInt((int) totalCount);
        var pageable = PageRequest.of(randomIndex, 1);

        Recording randomRecording = recordingRepository
                .findRootRecordingAtOffset(currentUserId, pageable)
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

        PeerFeedback feedback = PeerFeedback.builder()
                .recording(recording)
                .user(evaluator)
                .body(request.getBody())
                .followUpQuestion(request.getFollowUpQuestion())
                .build();

        peerFeedbackRepository.save(feedback);
    }
    private final GetUserService getUserService;
    /**
     * 내가 작성한 평가 상세 조회
     */
    public PeerFeedbackResponse getFeedbackDetail(Long feedbackId) {

        Long currentUserId = getUserService.getUserId();
        PeerFeedback feedback = peerFeedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new ApiException(ErrorCode.PEER_FEEDBACK_NOT_FOUND));


        if (!feedback.getUser().getId().equals(currentUserId)) {
            throw new ApiException(ErrorCode.FORBIDDEN_RESOURCE);
        }

        Recording recording = feedback.getRecording();
        InterviewQuestion question = recording.getInterviewQuestion();
        InterviewSession session = question.getInterviewSession();

        String recordingUrl = interviewObjectReadService.createRecordingGetUrl(recording.getObjectKey());


        return new PeerFeedbackResponse(
                feedback.getId(),
                question.getQuestion(),
                feedback.getBody(),
                feedback.getFollowUpQuestion(),
                session.getJobRole(),
                feedback.getCreatedAt()
        );
    }
}
