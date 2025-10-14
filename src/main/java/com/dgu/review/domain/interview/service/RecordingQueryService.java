package com.dgu.review.domain.interview.service;

import com.dgu.review.domain.interview.dto.response.NextPayload;
import com.dgu.review.domain.interview.dto.response.NextQuestionType;
import com.dgu.review.domain.interview.dto.response.ProgressStatus;
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

        InterviewQuestion current = recording.getInterviewQuestion();
        InterviewQuestion root = getRoot(current);
        var status = statusService.getStatus(recordingId);

        if (status == RecordingStatus.FAILED || recording.getFailedAt() != null) {
            return RecordingResultsResponse.builder()
                    .sessionId(current.getInterviewSession().getId())
                    .status(ProgressStatus.FAILED)
                    .next(null)
                    .build();
        }

        boolean isSuccess = (status == RecordingStatus.FOLLOWUP_GENERATED) ||
                (status == null && recording.getFailedAt() == null);

        if (isSuccess) {
            var next = decideNextPayload(current, root);
            return RecordingResultsResponse.builder()
                    .sessionId(current.getInterviewSession().getId())
                    .status(ProgressStatus.READY)
                    .next(next)
                    .build();
        }

        return RecordingResultsResponse.builder()
                .sessionId(current.getInterviewSession().getId())
                .status(ProgressStatus.WORKING)
                .next(null)
                .build();

    }

    private NextPayload decideNextPayload(InterviewQuestion current, InterviewQuestion currentRoot) {
        var savedFollowUp = current.getFollowUpQuestion();

        // 꼬리질문 존재
        if (savedFollowUp != null) {
            return NextPayload.builder()
                    .type(NextQuestionType.FOLLOW_UP)
                    .nextQuestionId(savedFollowUp.getId())
                    .nextQuestionText(savedFollowUp.getQuestion())
                    .rootId(currentRoot.getId())
                    .rootText(currentRoot.getQuestion())
                    .rootIndex(currentRoot.getQuestionNumber())
                    .build();
        }

        // 꼬리질문 x -> 다음 루트 질문
        var nextRoot = findNextRoot(currentRoot);
        if (nextRoot != null) {
            return NextPayload.builder()
                    .type(NextQuestionType.ROOT)
                    .nextQuestionId(nextRoot.getId())
                    .nextQuestionText(nextRoot.getQuestion())
                    .rootId(nextRoot.getId())
                    .rootText(nextRoot.getQuestion())
                    .rootIndex(nextRoot.getQuestionNumber())
                    .build();
        }

        // 더 이상 낼 질문이 없으면 NONE - 세션 종료
        return NextPayload.builder()
                .type(NextQuestionType.NONE)
                .rootId(currentRoot.getId())
                .rootText(currentRoot.getQuestion())
                .rootIndex(currentRoot.getQuestionNumber())
                .build();
    }

    private InterviewQuestion findNextRoot(InterviewQuestion root) {
        return root.getInterviewSession().getQuestions().stream()
                .filter(q -> q.getParentQuestion() == null)
                .filter(q -> q.getQuestionNumber() == root.getQuestionNumber() + 1)
                .findFirst()
                .orElse(null);
    }

    private InterviewQuestion getRoot(InterviewQuestion q) {
        var cur = q;
        while (cur.getParentQuestion() != null) cur = cur.getParentQuestion();
        return cur;
    }

}
