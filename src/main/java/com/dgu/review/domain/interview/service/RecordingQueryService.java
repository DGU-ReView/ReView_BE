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

    private static final int TOTAL_ROOTS = 4;

    private final RecordingRepository recordingRepo;
    private final RecordingStatusService statusService;

    public RecordingResultsResponse getRecordingResults(Long recordingId) {
        var recording = recordingRepo.findById(recordingId)
                .orElseThrow(() -> new ApiException(ErrorCode.RECORDING_NOT_FOUND));

        InterviewQuestion current = recording.getInterviewQuestion();
        InterviewQuestion root = getRoot(current);

        var status = statusService.getStatus(recordingId);
        var text = (status == RecordingStatus.COMPLETED || status == RecordingStatus.FOLLOWUP_GENERATED) ? recording.getSttText() : null;

        var savedFollowUp = current.getFollowUpQuestion();
        boolean followUpGenerated = (status == RecordingStatus.FOLLOWUP_GENERATED);

        boolean followUpDone = followUpGenerated && savedFollowUp == null;

        Long nextQuestionId = null;
        String followUpQuestion = null;

        if (followUpGenerated) {
            if (followUpDone) {
                nextQuestionId = findNextRootId(root);
            } else {
                nextQuestionId = savedFollowUp.getId();
                followUpQuestion = savedFollowUp.getQuestion();
            }
        }

        boolean sessionCompleted = followUpDone && (nextQuestionId == null || root.getQuestionNumber() == TOTAL_ROOTS);

        return new RecordingResultsResponse(
                nextQuestionId,
                status,
                text,
                followUpQuestion,
                followUpDone,
                new ContextStatus(
                        current.getInterviewSession().getId(),
                        sessionCompleted,
                        root.getQuestionNumber()
                ));
    }

    private Long findNextRootId(InterviewQuestion root) {
        return root.getInterviewSession().getQuestions().stream()
                .filter(q -> q.getParentQuestion() == null)
                .filter(q -> q.getQuestionNumber() == root.getQuestionNumber() + 1)
                .findFirst()
                .map(InterviewQuestion::getId)
                .orElse(null);
    }

    private InterviewQuestion getRoot(InterviewQuestion q) {
        var cur = q;
        while (cur.getParentQuestion() != null) cur = cur.getParentQuestion();
        return cur;
    }

}
