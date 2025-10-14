package com.dgu.review.domain.interview.dto.response;

public enum ProgressStatus {
    WORKING, // stt, 꼬리질문 생성 진행중
    READY, // 다음 질문 렌더 가능 (FOLLOWUP_GENERATED)
    FAILED
}
