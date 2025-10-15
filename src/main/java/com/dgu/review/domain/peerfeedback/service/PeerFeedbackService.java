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
import com.dgu.review.global.exception.ApiException;
import com.dgu.review.global.exception.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class PeerFeedbackService {

    private final PeerFeedbackRepository peerFeedbackRepository;
    private final RecordingRepository recordingRepository;

    /**
     * 랜덤 녹음 조회
     */
    public RandomRecordingResponse getRandomRecording(Long currentUserId) {
        Recording randomRecording = recordingRepository.findRandomRootRecording(currentUserId)
                .orElseThrow(() -> new ApiException(ErrorCode.RECORDING_NOT_FOUND, "평가할 녹음을 찾을 수 없습니다."));

        InterviewQuestion question = randomRecording.getInterviewQuestion();
        InterviewSession session = question.getInterviewSession();
        User interviewee = session.getUser();

        return new RandomRecordingResponse(
                randomRecording.getId(),
                question.getQuestion(),
                randomRecording.getSttText(),
                session.getJobRole()
        );
    }

    /**
     * 타인평가 저장
     */
    public PeerFeedbackResponse createFeedback(Long recordingId, PeerFeedbackRequest request, User evaluator) {
        Recording recording = recordingRepository.findById(recordingId)
                .orElseThrow(() -> new ApiException(ErrorCode.RECORDING_NOT_FOUND, "녹음 정보를 찾을 수 없습니다.")); // ✅ 수정

        // 자기 자신 평가 방지
        if (recording.getInterviewQuestion().getInterviewSession().getUser().getId().equals(evaluator.getId())) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "자기 자신의 답변은 평가할 수 없습니다."); // ✅ 수정
        }

        PeerFeedback feedback = PeerFeedback.builder()
                .recording(recording)
                .user(evaluator)
                .body(request.getBody())
                .followUpQuestion(request.getFollowUpQuestion())
                .build();

        peerFeedbackRepository.save(feedback);

        return new PeerFeedbackResponse(
                feedback.getId(),
                recording.getId(),
                evaluator.getId(),
                recording.getInterviewQuestion().getInterviewSession().getUser().getId(),
                feedback.getBody(),
                feedback.getFollowUpQuestion(),
                feedback.getCreatedAt()
        );
    }

    /**
     * 내가 작성한 평가 상세 조회
     */
    public PeerFeedbackResponse getFeedbackDetail(Long feedbackId) {
        PeerFeedback feedback = peerFeedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new ApiException(ErrorCode.PEER_FEEDBACK_NOT_FOUND, "피드백 정보를 찾을 수 없습니다.")); // ✅ 새 항목 필요

        Recording recording = feedback.getRecording();
        InterviewQuestion question = recording.getInterviewQuestion();
        Long intervieweeId = question.getInterviewSession().getUser().getId();

        return new PeerFeedbackResponse(
                feedback.getId(),
                recording.getId(),
                feedback.getUser().getId(),
                intervieweeId,
                feedback.getBody(),
                feedback.getFollowUpQuestion(),
                feedback.getCreatedAt()
        );
    }
}
