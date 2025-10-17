package com.dgu.review.domain.interview.service;

import com.dgu.review.domain.interview.dto.response.GetInterviewResultsResponse;
import com.dgu.review.domain.interview.dto.response.InterviewSummary;
import com.dgu.review.domain.interview.dto.response.ProgressStatus;
import com.dgu.review.domain.interview.dto.response.QuestionSummary;
import com.dgu.review.domain.interview.entity.InterviewSession;
import com.dgu.review.domain.interview.repository.InterviewQuestionRepository;
import com.dgu.review.domain.interview.repository.InterviewSessionRepository;
import com.dgu.review.domain.interview.repository.RecordingRepository;
import com.dgu.review.global.exception.ApiException;
import com.dgu.review.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

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
            var stt = root.getRecording().getSttText();
            boolean hasRootAnswer = stt != null && !stt.isBlank();

            boolean feedbackMissing =
                    (root.getAiFeedback() == null || root.getAiFeedback().isBlank())
                    || (root.getSelfFeedback() == null || root.getSelfFeedback().isBlank());

            return hasRootAnswer && feedbackMissing;
        });

        if (anyNeedingFeedback) {
            return new GetInterviewResultsResponse(ProgressStatus.WORKING, null);
        }

        List<QuestionSummary> questionSummaries =
                interviewQuestionRepository.findOrderedRootSummaries(sessionId).stream()
                        .map(v -> QuestionSummary.builder()
                                .questionNumber(v.getQuestionNumber())
                                .rootQuestion(v.getRootQuestion())
                                .sttText(v.getSttText())
                                .aiFeedback(v.getAiFeedback())
                                .selfFeedback(v.getSelfFeedback())
                                .build()
                        ).toList();

        InterviewSummary interviewSummary = InterviewSummary.builder()
                .interviewTitle(session.getTitle())
                .timeoutQuestionNumber(recordingRepository.countTimeoutsBySessionId(sessionId))
                .questionSummaries(questionSummaries)
                .build();

        return new GetInterviewResultsResponse(ProgressStatus.READY, interviewSummary);


    }
}
