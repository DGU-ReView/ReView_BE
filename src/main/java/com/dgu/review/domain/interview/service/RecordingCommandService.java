package com.dgu.review.domain.interview.service;

import com.dgu.review.domain.interview.dto.request.RecordingCreateRequest;
import com.dgu.review.domain.interview.dto.response.RecordingCreateResponse;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class RecordingService {

    private final RecordingRepository recordingRepo;
    private final RecordingStatusService statusService;
    private final RecordingJobQueue recordingJobQueue;

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public RecordingCreateResponse createAndTranscribe(Long questionId, RecordingCreateRequest request) {
        Recording saved = saveRecording(questionId, request);
        return enqueueRecordingJob(saved);
    }

    @Transactional
    public RecordingCreateResponse enqueueRecordingJob(Recording recording) {

        var cur = statusService.getStatus(recording.getId());
        if (cur == RecordingStatus.FAILED || cur == null) {
            if (cur == RecordingStatus.FAILED) {
                statusService.clearStatus(recording.getId());
            }
            boolean first = statusService.trySetUploadedIfAbsent(recording.getId());

            if (!first) {
                throw new ApiException(ErrorCode.ALREADY_IN_QUEUE_OR_DONE);
            }

            log.info("[enqueue] dispatch async worker recordingId={}", recording.getId());

            recordingJobQueue.enqueue(recording);

            return new RecordingCreateResponse(recording.getId(), statusService.getStatus(recording.getId()).name());

        }

        throw new ApiException(ErrorCode.ALREADY_IN_QUEUE_OR_DONE);
    }


    private Recording saveRecording(Long questionId, RecordingCreateRequest req) {
        var question = em.find(InterviewQuestion.class, questionId);
        if (question == null) {
            log.warn("인터뷰 질문을 찾을 수 없습니다. interviewQuestionId={}", questionId);
            throw new ApiException(ErrorCode.INTERVIEW_QUESTION_NOT_FOUND);
        }

        var existingRecording = recordingRepo.findByInterviewQuestion(question);
        if (existingRecording.isPresent()) {
            Recording r = existingRecording.get();

            if (r.getSttText() != null && !r.getSttText().isEmpty()) {
                throw new ApiException(ErrorCode.RECORDING_ALREADY_PROCESSED);
            }

            throw new ApiException(ErrorCode.ALREADY_IN_QUEUE_OR_DONE);
        }

        var rec = Recording.builder()
                .objectKey(req.getObjectKey())
                .sttText("")
                .interviewQuestion(question)
                .build();

        question.attachRecording(rec);
        return recordingRepo.save(rec);
    }
}