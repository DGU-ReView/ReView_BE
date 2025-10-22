package com.dgu.review.domain.interview.service;

import com.dgu.review.domain.interview.dto.response.GetRandomQuestionFeedbackResponse;
import com.dgu.review.domain.interview.dto.response.ProgressStatus;
import com.dgu.review.domain.interview.dto.response.RandomQuestionFeedbackResult;
import com.dgu.review.domain.interview.dto.response.GetRandomQuestionResponse;
import com.dgu.review.domain.interview.entity.FeedbackQuestion;
import com.dgu.review.domain.interview.entity.InterviewQuestion;
import com.dgu.review.domain.interview.entity.Recording;
import com.dgu.review.domain.interview.entity.RecordingStatus;
import com.dgu.review.domain.interview.repository.FeedbackQuestionRepository;
import com.dgu.review.domain.interview.repository.RecordingRepository;
import com.dgu.review.domain.peerfeedback.entity.PeerFeedback;
import com.dgu.review.domain.peerfeedback.repository.PeerFeedbackRepository;
import com.dgu.review.global.exception.ApiException;
import com.dgu.review.global.exception.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class RandomQuestionService {

    private final PeerFeedbackRepository peerFeedbackRepository;
    private final RecordingRepository recordingRepository;
    private final InterviewObjectReadService interviewObjectReadService;
    private final RecordingStatusService recordingStatusService;
    private final FeedbackQuestionRepository feedbackQuestionRepository;

    @Transactional
    public GetRandomQuestionResponse getRandomQuestion(Long peerAnswerId) {
        PeerFeedback peerFeedback = peerFeedbackRepository.findById(peerAnswerId)
                .orElseThrow(() -> new ApiException(ErrorCode.PEER_FEEDBACK_NOT_FOUND));

        InterviewQuestion originalQuestion = peerFeedback.getRecording().getInterviewQuestion();

        FeedbackQuestion feedbackQuestion = FeedbackQuestion.builder()
                .question(peerFeedback.getFollowUpQuestion())
                .interviewSession(originalQuestion.getInterviewSession())
                .parentQuestion(originalQuestion)
                .sourcePeerFeedback(peerFeedback)
                .build();

        feedbackQuestionRepository.save(feedbackQuestion);
        originalQuestion.attachFeedbackQuestion(feedbackQuestion);
        peerFeedback.attachGeneratedQuestion(feedbackQuestion);

        GetRandomQuestionResponse.Question question = new GetRandomQuestionResponse.Question(
                feedbackQuestion.getId(),
                feedbackQuestion.getQuestion()
        );

        GetRandomQuestionResponse.Context context = new GetRandomQuestionResponse.Context(
                originalQuestion.getId(),
                originalQuestion.getQuestion(),
                peerFeedback.getRecording().getObjectKey(),
                peerFeedback.getRecording().getSttText()
        );

        return new GetRandomQuestionResponse(question, context);
    }

    public GetRandomQuestionFeedbackResponse getRandomQuestionFeedback(Long recordingId) {
        Recording recording = recordingRepository.findById(recordingId)
                .orElseThrow(() -> new ApiException(ErrorCode.RECORDING_NOT_FOUND));

        if (recording.getFailedAt() != null) {
            return new GetRandomQuestionFeedbackResponse(ProgressStatus.FAILED, null);
        }

        FeedbackQuestion feedbackQuestion = recording.getFeedbackQuestion();
        if (feedbackQuestion == null) {
            throw new ApiException(ErrorCode.INVALID_FEEDBACK_RECORDING);
        }

        if (recordingStatusService.getStatus(recordingId) == RecordingStatus.FOLLOWUP_GENERATED
        && recording.getFeedbackQuestion().getAiFeedback() != null
        && recording.getFeedbackQuestion().getSelfFeedback() != null) {
            String presignedRecordingGetUrl = interviewObjectReadService.createRecordingGetUrl(recording.getObjectKey());

            RandomQuestionFeedbackResult result = RandomQuestionFeedbackResult.builder()
                    .questionId(feedbackQuestion.getId())
                    .questionText(feedbackQuestion.getQuestion())
                    .aiFeedback(feedbackQuestion.getAiFeedback())
                    .selfFeedback(feedbackQuestion.getSelfFeedback())
                    .presignedRecordingGetUrl(presignedRecordingGetUrl)
                    .sttText(recording.getSttText())
                    .build();

            return new GetRandomQuestionFeedbackResponse(ProgressStatus.READY, result);
        }

        return new GetRandomQuestionFeedbackResponse(ProgressStatus.WORKING, null);


    }



}
