package com.dgu.review.domain.interview.service;

import com.dgu.review.domain.interview.dto.response.GetRandomQuestionFeedbackResponse;
import com.dgu.review.domain.interview.dto.response.ProgressStatus;
import com.dgu.review.domain.interview.dto.response.RandomQuestionFeedbackResult;
import com.dgu.review.domain.interview.dto.response.GetRandomQuestionResponse;
import com.dgu.review.domain.interview.entity.InterviewQuestion;
import com.dgu.review.domain.interview.entity.Recording;
import com.dgu.review.domain.interview.entity.RecordingStatus;
import com.dgu.review.domain.interview.repository.InterviewQuestionRepository;
import com.dgu.review.domain.interview.repository.RecordingRepository;
import com.dgu.review.domain.peerfeedback.entity.PeerFeedback;
import com.dgu.review.domain.peerfeedback.repository.PeerFeedbackRepository;
import com.dgu.review.global.exception.ApiException;
import com.dgu.review.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class RandomQuestionService {

    private final PeerFeedbackRepository peerFeedbackRepository;
    private final RecordingRepository recordingRepository;
    private final InterviewObjectReadService interviewObjectReadService;
    private final InterviewQuestionRepository interviewQuestionRepository;
    private final RecordingStatusService recordingStatusService;

    public GetRandomQuestionResponse getRandomQuestion(Long peerAnswerId) {
        PeerFeedback peerFeedback = peerFeedbackRepository.findById(peerAnswerId)
                .orElseThrow(() -> new ApiException(ErrorCode.PEER_FEEDBACK_NOT_FOUND));

        InterviewQuestion interviewQuestion = interviewQuestionRepository.save(InterviewQuestion.builder()
                .question(peerFeedback.getFollowUpQuestion())
                .interviewSession(peerFeedback.getRecording().getInterviewQuestion().getInterviewSession())
                .parentQuestion(peerFeedback.getRecording().getInterviewQuestion())
                .sourcePeerFeedback(peerFeedback)
                .build());

        GetRandomQuestionResponse.Question question = new GetRandomQuestionResponse.Question(
                interviewQuestion.getId(),
                interviewQuestion.getQuestion()
        );

        GetRandomQuestionResponse.Context context = new GetRandomQuestionResponse.Context(
                peerFeedback.getRecording().getInterviewQuestion().getId(),
                peerFeedback.getRecording().getInterviewQuestion().getQuestion(),
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

        if (recordingStatusService.getStatus(recordingId) == RecordingStatus.FOLLOWUP_GENERATED
        && recording.getInterviewQuestion().getAiFeedback() != null
        && recording.getInterviewQuestion().getSelfFeedback() != null) {
            String presignedRecordingGetUrl = interviewObjectReadService.createRecordingGetUrl(recording.getObjectKey());

            RandomQuestionFeedbackResult result = RandomQuestionFeedbackResult.builder()
                    .questionId(recording.getInterviewQuestion().getId())
                    .questionText(recording.getInterviewQuestion().getQuestion())
                    .aiFeedback(recording.getInterviewQuestion().getAiFeedback())
                    .selfFeedback(recording.getInterviewQuestion().getSelfFeedback())
                    .presignedRecordingGetUrl(presignedRecordingGetUrl)
                    .sttText(recording.getSttText())
                    .build();

            return new GetRandomQuestionFeedbackResponse(ProgressStatus.READY, result);
        }

        return new GetRandomQuestionFeedbackResponse(ProgressStatus.WORKING, null);


    }



}
