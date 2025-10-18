package com.dgu.review.domain.interview.service;

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
    private final NextQuestionPlanner nextQuestionPlanner;

    public RecordingResultsResponse getRecordingResults(Long recordingId) {
        var recording = recordingRepo.findById(recordingId)
                .orElseThrow(() -> new ApiException(ErrorCode.RECORDING_NOT_FOUND));

        InterviewQuestion current = recording.getInterviewQuestion();
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
            var next = nextQuestionPlanner.decideNextPayload(current);
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

}
