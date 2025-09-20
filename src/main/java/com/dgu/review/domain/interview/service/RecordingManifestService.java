package com.dgu.review.domain.interview.service;

import com.dgu.review.domain.interview.dto.RecordingManifestCreateRequest;
import com.dgu.review.domain.interview.dto.RecordingManifestCreateResponse;
import com.dgu.review.domain.interview.dto.RecordingManifestDetailResponse;
import com.dgu.review.domain.interview.entity.InterviewQuestion;
import com.dgu.review.domain.interview.entity.Recording;
import com.dgu.review.domain.interview.repository.RecordingRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

//녹음 등록과 조회를 처리하는 로직
@Service
@RequiredArgsConstructor
public class RecordingManifestService {

    private final RecordingRepository recordingRepo; //DB에 Recording 엔티티를 저장/조회하는 JPA

    @PersistenceContext
    private EntityManager em; // setter 없이 FK 연결하기 위해 사용

    @Transactional
    public RecordingManifestCreateResponse create(RecordingManifestCreateRequest req) {
        // 새로운 녹음을 등록하는 메서드.
        InterviewQuestion qRef = em.getReference(InterviewQuestion.class, req.getInterviewQuestionId());

        Recording rec = Recording.builder()
                .objectKey(req.getObjectKey()) //업로도된 음성 파일의 저장 위치
                .sttText("")             // nullable=false → 비어있는 문자열로 초기화
                .interviewQuestion(qRef) // FK 연결
                .build();

        Recording saved = recordingRepo.save(rec); //Recoding 엔티티를 DB에 저장
        return new RecordingManifestCreateResponse(saved.getId(), deriveStatus(saved));
    }

    @Transactional //특정 녹음 ID로 상세 정보 조회
    public RecordingManifestDetailResponse get(Long id) {
        Recording r = recordingRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Recording not found"));
        return new RecordingManifestDetailResponse(
                r.getId(), r.getObjectKey(), deriveStatus(r) //recordingId, objectKey, status만
        );
    }

    // 상태 파생: ""=UPLOADED, "__TRANSCRIBING__"=TRANSCRIBING, 그 외=SUCCEEDED
    private String deriveStatus(Recording r) {
        String t = r.getSttText(); //DB에 저장된 sttText 컬럼 가져옴
        if (t == null || t.isEmpty()) return "UPLOADED";
        if ("__TRANSCRIBING__".equals(t)) return "TRANSCRIBING"; //전사 시작
        return "SUCCEEDED"; //
    }
}
// 녹음 등록: 요청 DTO를 받아 Recoding entity 생성하고 DB저장 후 응답 DTO 반환
// 녹음 조회: DB에서 Recoding 찾고 응답 DTO 반환
// 상태: DB 칼럼만 보고 상태 유추