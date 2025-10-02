package com.dgu.review.domain.interview.service;

import com.dgu.review.domain.interview.dto.request.RecordingCreateRequest;
import com.dgu.review.domain.interview.dto.response.GetFollowUpQuestionResponse;
import com.dgu.review.domain.interview.dto.response.RecordingCreateResponse;
//import com.dgu.review.domain.interview.dto.RecordingManifestDetailResponse;
import com.dgu.review.domain.interview.dto.response.SttFeedbackResponse;
import com.dgu.review.domain.interview.entity.InterviewQuestion;
import com.dgu.review.domain.interview.entity.Recording;
import com.dgu.review.domain.interview.entity.RecordingStatus;
import com.dgu.review.domain.interview.repository.InterviewQuestionRepository;
import com.dgu.review.domain.interview.repository.RecordingRepository;
import com.dgu.review.global.exception.ApiException;
import com.dgu.review.global.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

//녹음 등록과 조회를 처리하는 로직
@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewSessionService {

    private final RecordingRepository recordingRepo; //DB에 Recording 엔티티를 저장/조회하는 JPA
    private final SttService sttService;
    private final RecordingStatusService statusService;
    private final SttFeedbackService sttFeedbackService;
    private final InterviewQuestionRepository interviewQuestionRepository;
    private final RecordingJobQueue recordingJobQueue;

    @PersistenceContext
    private EntityManager em; // setter 없이 FK 연결하기 위해 사용

    @Transactional
    public RecordingCreateResponse createAndTranscribe(Long sessionId, Long questionId, RecordingCreateRequest request) {
        Recording saved = saveRecording(sessionId, questionId, request);
        return enqueueRecordingJob(saved);
    }
    /*
        @Transactional
    public RecordingManifestCreateResponse create(RecordingManifestCreateRequest req) {

        InterviewQuestion question = em.find(InterviewQuestion.class, req.getInterviewQuestionId());
        if (question == null) {
            //예외처리 로직 추가
            throw new IllegalArgumentException("InterviewQuestion not found with id: " + req.getInterviewQuestionId());
        }

        Recording rec = Recording.builder()
                .objectKey(req.getObjectKey()) // 업로드된 음성 파일의 저장 위치
                .sttText("")                   // nullable=false → 비어있는 문자열로 초기화
                .interviewQuestion(question)   // FK 연결
                .build();

        Recording saved = recordingRepo.save(rec); // Recording 엔티티를 DB에 저장
        return new RecordingManifestCreateResponse(saved.getId(), deriveStatus(saved));
    }
     */

/*
    @Transactional
    public RecordingManifestDetailResponse get(Long id) {
        Recording r = recordingRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Recording not found"));

        RecordingStatus status = statusService.getStatus(r.getId());
        return new RecordingManifestDetailResponse(r.getId(), r.getObjectKey(), status.name());
    }
 */


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
            // STT 비동기 실행
            recordingJobQueue.enqueue(recording);

            return new RecordingCreateResponse(recording.getId(), statusService.getStatus(recording.getId()).name());

        }

        throw new ApiException(ErrorCode.ALREADY_IN_QUEUE_OR_DONE);
    }


    private Recording saveRecording(Long sessionId, Long questionId, RecordingCreateRequest req) {
        var question = em.find(InterviewQuestion.class, questionId);
        if (question == null) {
            log.warn("인터뷰 질문을 찾을 수 없습니다. interviewQuestionId={}", questionId);
            throw new ApiException(ErrorCode.INTERVIEW_QUESTION_NOT_FOUND);
        }
        var qSessionId = question.getInterviewSession().getId();
        if (!qSessionId.equals(sessionId)) {
            log.warn("질문 세션 불일치. requestSessionId={}, questionSessionId={}", sessionId, qSessionId);
            throw new ApiException(ErrorCode.INTERVIEW_SESSION_MISMATCH);
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
// 녹음 등록: 요청 DTO를 받아 Recoding entity 생성하고 DB저장 후 응답 DTO 반환
// 녹음 조회: DB에서 Recoding 찾고 응답 DTO 반환
// 상태: DB 칼럼만 보고 상태 유추