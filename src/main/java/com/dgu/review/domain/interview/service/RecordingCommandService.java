package com.dgu.review.domain.interview.service;

import com.dgu.review.domain.interview.dto.response.NextPayload;
import com.dgu.review.domain.interview.dto.response.ProgressStatus;
import com.dgu.review.domain.interview.dto.response.RecordingCreateResponse;
import com.dgu.review.domain.interview.dto.response.RecordingResultsResponse;
import com.dgu.review.domain.interview.entity.InterviewQuestion;
import com.dgu.review.domain.interview.entity.Recording;
import com.dgu.review.domain.interview.entity.RecordingStatus;
import com.dgu.review.domain.interview.repository.RecordingRepository;
import com.dgu.review.global.exception.ApiException;
import com.dgu.review.global.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecordingCommandService {

    private final RecordingRepository recordingRepo;
    private final RecordingStatusService statusService;
    private final RecordingJobQueue recordingJobQueue;
    private final InterviewPresignService interviewPresignService;
    private final NextQuestionPlanner nextQuestionPlanner;

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public RecordingCreateResponse createAndTranscribe(Long questionId) {
        Recording saved = saveRecording(questionId);
        return enqueueRecordingJob(saved.getId());
    }

    @Transactional
    public RecordingResultsResponse markTimeout(Long questionId) {
        var question = em.find(InterviewQuestion.class, questionId);
        if (question == null) {
            throw new ApiException(ErrorCode.INTERVIEW_QUESTION_NOT_FOUND);
        }

        var existingOpt = recordingRepo.findByInterviewQuestion(question);
        Recording rec;

        if (existingOpt.isPresent()) {
            rec = existingOpt.get();

            if (rec.getSttText() != null && !rec.getSttText().isBlank()) {
                throw new ApiException(ErrorCode.RECORDING_ALREADY_PROCESSED);
            }

            rec.attachSttText(null);
        } else {
            rec = Recording.builder()
                    .objectKey(null)
                    .sttText(null)
                    .interviewQuestion(question)
                    .build();
            question.attachRecording(rec);
            rec = recordingRepo.save(rec);

        }

        statusService.setStatus(rec.getId(), RecordingStatus.TIMEOUT, null);

        NextPayload next = nextQuestionPlanner.decideNextPayload(question);

        log.info("[timeout] questionId={}, sessionId={}, userId={}, nextQuestionId={}",
                questionId,
                question.getInterviewSession().getId(),
                question.getInterviewSession().getUser().getId(),
                next.nextQuestionId());

        return RecordingResultsResponse.builder()
                .sessionId(question.getInterviewSession().getId())
                .status(ProgressStatus.READY)
                .next(next)
                .build();
    }

    @Transactional
    public RecordingCreateResponse enqueueRecordingJob(Long recordingId) {

        var cur = statusService.getStatus(recordingId);
        if (cur == RecordingStatus.FAILED || cur == RecordingStatus.TIMEOUT || cur == null) {
            if (cur == RecordingStatus.FAILED || cur == RecordingStatus.TIMEOUT) {
                statusService.clearStatus(recordingId);
            }
            boolean first = statusService.trySetUploadedIfAbsent(recordingId);

            if (!first) {
                throw new ApiException(ErrorCode.ALREADY_IN_QUEUE_OR_DONE);
            }

            log.info("[enqueue] dispatch async worker recordingId={}", recordingId);

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() {
                    recordingJobQueue.enqueue(recordingId);
                }
            });

            return new RecordingCreateResponse(recordingId, statusService.getStatus(recordingId).name());

        }

        throw new ApiException(ErrorCode.ALREADY_IN_QUEUE_OR_DONE);
    }


    private Recording saveRecording(Long questionId) {
        var question = em.find(InterviewQuestion.class, questionId);
        if (question == null) {
            log.warn("인터뷰 질문을 찾을 수 없습니다. interviewQuestionId={}", questionId);
            throw new ApiException(ErrorCode.INTERVIEW_QUESTION_NOT_FOUND);
        }

        var existingRecording = recordingRepo.findByInterviewQuestion(question);
        if (existingRecording.isPresent()) {
            Recording r = existingRecording.get();

            if (r.getSttText() != null && !r.getSttText().isBlank()) {
                throw new ApiException(ErrorCode.RECORDING_ALREADY_PROCESSED);
            }

            r.updateObjectKey(interviewPresignService.getRecordingObjectKey(questionId));
            r.attachSttText("");
            return r;
        }

        var rec = Recording.builder()
                .objectKey(interviewPresignService.getRecordingObjectKey(questionId))
                .sttText("")
                .interviewQuestion(question)
                .build();

        question.attachRecording(rec);
        return recordingRepo.save(rec);
    }
}