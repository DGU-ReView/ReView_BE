package com.dgu.review.domain.interview.service;

import com.dgu.review.domain.interview.dto.RecordingCreateRequest;
import com.dgu.review.domain.interview.dto.RecordingCreateResponse;
//import com.dgu.review.domain.interview.dto.RecordingManifestDetailResponse;
import com.dgu.review.domain.interview.entity.InterviewQuestion;
import com.dgu.review.domain.interview.entity.Recording;
import com.dgu.review.domain.interview.entity.RecordingStatus;
import com.dgu.review.domain.interview.repository.RecordingRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

//녹음 등록과 조회를 처리하는 로직
@Service
@RequiredArgsConstructor
public class InterviewSessionService {

    private final RecordingRepository recordingRepo; //DB에 Recording 엔티티를 저장/조회하는 JPA
    private final SttService sttService;
    private final RecordingStatusService statusService;

    @PersistenceContext
    private EntityManager em; // setter 없이 FK 연결하기 위해 사용

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
    public RecordingCreateResponse createAndTranscribe(Long sessionId, RecordingCreateRequest req) { // ✅ sessionId 추가
        Recording saved = saveRecording(sessionId, req);

        statusService.setStatus(saved.getId(), RecordingStatus.UPLOADED);
        sttService.enqueue(saved.getId());
        return new RecordingCreateResponse(saved.getId(), RecordingStatus.TRANSCRIBING.name());
    }

    private Recording saveRecording(Long sessionId, RecordingCreateRequest req) {
        var question = em.find(InterviewQuestion.class, req.getInterviewQuestionId());
        if (question == null) throw new IllegalArgumentException("InterviewQuestion not found: " + req.getInterviewQuestionId());


        var qSessionId = question.getInterviewSession().getId();
        if (!qSessionId.equals(sessionId)) {
            throw new IllegalArgumentException("Question does not belong to session: " + sessionId);
        }

        var rec = Recording.builder()
                .objectKey(req.getObjectKey())
                .sttText("")
                .interviewQuestion(question)
                .build();

        return recordingRepo.save(rec);
    }
}
// 녹음 등록: 요청 DTO를 받아 Recoding entity 생성하고 DB저장 후 응답 DTO 반환
// 녹음 조회: DB에서 Recoding 찾고 응답 DTO 반환
// 상태: DB 칼럼만 보고 상태 유추