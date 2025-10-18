package com.dgu.review.domain.interview.service;

import com.dgu.review.domain.interview.dto.response.*;
import com.dgu.review.domain.interview.entity.InterviewQuestion;
import com.dgu.review.domain.interview.entity.InterviewSession;
import com.dgu.review.domain.interview.repository.InterviewQuestionRepository;
import com.dgu.review.domain.interview.repository.InterviewSessionRepository;
import com.dgu.review.domain.interview.repository.RecordingRepository;
import com.dgu.review.global.exception.ApiException;
import com.dgu.review.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InterviewService {

    private final InterviewSessionRepository interviewSessionRepository;
    private final InterviewQuestionRepository interviewQuestionRepository;
    private final RecordingRepository recordingRepository;

    public GetInterviewResultsResponse getInterviewResults(Long sessionId) {
        InterviewSession session = interviewSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ApiException(ErrorCode.INTERVIEW_SESSION_NOT_FOUND));

        var roots = interviewQuestionRepository.findRootsBySessionId(sessionId);

        boolean anyNeedingFeedback = roots.stream().anyMatch(root -> {
            var rec = root.getRecording();
            String stt = (rec != null) ? rec.getSttText() : null;
            boolean hasRootAnswer = stt != null && !stt.isBlank();

            boolean feedbackMissing =
                    (root.getAiFeedback() == null || root.getAiFeedback().isBlank())
                    || (root.getSelfFeedback() == null || root.getSelfFeedback().isBlank());

            return hasRootAnswer && feedbackMissing;
        });

        if (anyNeedingFeedback) {
            return new GetInterviewResultsResponse(ProgressStatus.WORKING, null);
        }

        List<QuestionSummary> questionSummaries = roots.stream()
                .map(root -> {
                    List<QnATurn> qnaTurns = buildQnATurns(root);

                    return QuestionSummary.builder()
                            .questionNumber(root.getQuestionNumber())
                            .rootQuestion(root.getQuestion())
                            .aiFeedback(root.getAiFeedback())
                            .selfFeedback(root.getSelfFeedback())
                            .qnaTurns(qnaTurns)
                            .build();
                }).toList();


        InterviewSummary interviewSummary = InterviewSummary.builder()
                .interviewTitle(session.getTitle())
                .timeoutQuestionNumber(recordingRepository.countTimeoutsBySessionId(sessionId))
                .questionSummaries(questionSummaries)
                .build();

        return new GetInterviewResultsResponse(ProgressStatus.READY, interviewSummary);

    }

    private List<QnATurn> buildQnATurns(InterviewQuestion rootQuestion) {
        List<QnATurn> turns = new ArrayList<>();
        InterviewQuestion currentQuestion = rootQuestion;

        while (currentQuestion != null) {

            turns.add(new QnATurn(TurnType.QUESTION, currentQuestion.getQuestion()));

            var recording = currentQuestion.getRecording();
            if (recording != null && recording.getSttText() != null && !recording.getSttText().isBlank()) {
                turns.add(new QnATurn(TurnType.ANSWER, recording.getSttText()));
            }

            currentQuestion = currentQuestion.getFollowUpQuestion();
        }

        return turns;
    }
}
