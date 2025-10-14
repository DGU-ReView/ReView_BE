package com.dgu.review.domain.interview.dto.response;

public enum NextQuestionType {
    FOLLOW_UP,
    ROOT, // 다음 루트 질문
    NONE // 더 보여줄 게 없음 (세션 끝)
}
