package com.dgu.review.domain.interview.service;

import com.dgu.review.domain.interview.dto.response.ContextStatus;
import com.dgu.review.domain.interview.dto.response.RecordingResultsResponse;
import com.dgu.review.domain.interview.entity.InterviewQuestion;
import com.dgu.review.domain.interview.entity.RecordingStatus;
import com.dgu.review.domain.interview.repository.RecordingRepository;
import com.dgu.review.global.exception.ApiException;
import com.dgu.review.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecordingQueryService {

    private final RecordingRepository recordingRepo;
    private final RecordingStatusService statusService;

    public RecordingResultsResponse getRecordingResults(Long recordingId) {
        var recording = recordingRepo.findById(recordingId)
                .orElseThrow(() -> new ApiException(ErrorCode.RECORDING_NOT_FOUND));

        var status = statusService.getStatus(recordingId);
        var text = (status == RecordingStatus.COMPLETED || status == RecordingStatus.FOLLOWUP_GENERATED) ? recording.getSttText() : null;
        var question = recording.getInterviewQuestion().getFollowUpQuestion();
        String followUpQuestion =
                (status == RecordingStatus.FOLLOWUP_GENERATED && question != null)
                        ? question.getQuestion()
                        : null;

        boolean followUpDone =
                "추가 질문이 필요하지 않습니다.".equals(followUpQuestion);

        return new RecordingResultsResponse(recordingId, status, text, followUpQuestion, followUpDone,
                new ContextStatus(
                        recording.getInterviewQuestion().getInterviewSession().getId(), (isFourthRootQuestion(question) && followUpDone)
                ));
    }

    private boolean isFourthRootQuestion(InterviewQuestion question) {
        if (question == null) return false;

        InterviewQuestion current = question;
        while (current.getParentQuestion() != null) {
            current = current.getParentQuestion();
        }

        return current.getQuestionNumber() == 4;
    }

}
