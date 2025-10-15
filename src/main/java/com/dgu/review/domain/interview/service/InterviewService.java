package com.dgu.review.domain.interview.service;

import com.dgu.review.domain.interview.dto.response.GetInterviewResultsResponse;
import com.dgu.review.domain.interview.dto.response.QuestionSummary;
import com.dgu.review.domain.interview.entity.InterviewSession;
import com.dgu.review.domain.interview.repository.InterviewSessionRepository;
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

    public GetInterviewResultsResponse getInterviewResults(Long sessionId) {
        InterviewSession session = interviewSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ApiException(ErrorCode.INTERVIEW_SESSION_NOT_FOUND));

        List<QuestionSummary> questionSummaries = session.getQuestions().stream()
                .filter(q -> q.getParentQuestion() == null)
                .map(q -> QuestionSummary.builder()
                        .questionNumber(q.getQuestionNumber())
                        .rootQuestion(q.getQuestion())
                        .sttText(q.getRecording().getSttText())
                        .aiFeedback(q.getAiFeedback())
                        .selfFeedback(q.getSelfFeedback())
                        .build()
                ).collect(Collectors.toList());

        return GetInterviewResultsResponse.builder()
                .interviewTitle(session.getTitle())
                .timeoutQuestionNumber(session.getTimeoutQuestionNumber())
                .questionSummaries(questionSummaries)
                .build();
    }
}
