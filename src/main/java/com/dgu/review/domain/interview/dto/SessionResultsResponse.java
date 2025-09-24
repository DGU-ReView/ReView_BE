package com.dgu.review.domain.interview.dto;

import java.util.List;

/**
 * 세션 단위 STT 상태 응답 DTO
 * - sessionId: 세션 식별자
 * - overallStatus: 세션 전체 상태 (UPLOADED / TRANSCRIBING / COMPLETE)
 * - recordings: 세션에 속한 개별 녹음들의 상태 리스트
 */
public record SessionResultsResponse(
        Long sessionId,
        String overallStatus,
        List<Item> recordings
) {
    /**
     * 세션 내 개별 녹음 정보
     * - recordingId: 녹음 식별자
     * - status: 개별 녹음 상태 (UPLOADED / TRANSCRIBING / COMPLETE)
     * - sttText: STT 변환 결과 텍스트 (COMPLETE 상태일 때만 채워짐, 그 외에는 null)
     */
    public record Item(
            Long recordingId,
            String status,
            String sttText
    ) {}
}