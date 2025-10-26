package com.dgu.review.domain.interview.service;

import com.dgu.review.domain.interview.dto.response.NextPayload;
import com.dgu.review.domain.interview.dto.response.NextQuestionType;
import com.dgu.review.domain.interview.entity.InterviewQuestion;
import com.dgu.review.global.exception.ApiException;
import com.dgu.review.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NextQuestionPlanner {

    public NextPayload decideNextPayload(InterviewQuestion current) {
        var currentRoot = getRoot(current);
        var savedFollowUp = current.getFollowUpQuestion();

        if (currentRoot.getQuestionNumber() == null) {
            log.warn("[NextQuestionPlanner] 'currentRoot' (ID: {}) has a null questionNumber. Session ID: {}",
                    currentRoot.getId(), currentRoot.getInterviewSession().getId());
        }

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
        if (root.getQuestionNumber() == null) {
            log.warn("[NextQuestionPlanner.findNextRoot] 'root' parameter (ID: {}) has a null questionNumber. Session ID: {}",
                    root.getId(), root.getInterviewSession().getId());
        }
        return root.getInterviewSession().getQuestions().stream()
                .filter(q -> q.getParentQuestion() == null)
                .peek(q -> {
                            if (q.getQuestionNumber() == null) {
                                log.warn("[NextQuestionPlanner.findNextRoot] Stream filter check: Found root question (ID: {}) with null questionNumber. Session ID: {}",
                                        q.getId(), root.getInterviewSession().getId());
                            }
                        }

                )
                .filter(q -> q.getQuestionNumber() == root.getQuestionNumber() + 1)
                .findFirst()
                .orElse(null);
    }

    private InterviewQuestion getRoot(InterviewQuestion q) {
        var cur = q;
        int depth = 0;

        while (cur.getParentQuestion() != null) {
            cur = cur.getParentQuestion();

            if (++depth > 10) {
                log.error("질문 데이터 순환 참조 의심. recordingId: {}", q.getRecording().getId());
                throw new ApiException(ErrorCode.DATA_INTEGRITY_VIOLATED);
            }
        }
        return cur;
    }
}
