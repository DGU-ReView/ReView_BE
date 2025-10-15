package com.dgu.review.domain.interview.service;

import com.dgu.review.domain.interview.entity.InterviewQuestion;
import com.dgu.review.domain.interview.repository.InterviewQuestionRepository;
import com.dgu.review.global.exception.ApiException;
import com.dgu.review.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedbackJob {

    private final InterviewQuestionRepository interviewQuestionRepository;
    private final SttFeedbackService sttFeedbackService;

    @Async
    @Transactional
    public void generateAiFeedbackAsync(Long rootQuestionId) {
        try {
            InterviewQuestion root = interviewQuestionRepository.findById(rootQuestionId)
                    .orElseThrow(() -> new ApiException(ErrorCode.INTERVIEW_QUESTION_NOT_FOUND));

            if (root.getAiFeedback() != null && !root.getAiFeedback().isBlank()) {
                //throw new ApiException(ErrorCode.AI_FEEDBACK_ALREADY_EXIST);
                log.info("[feedback] already exists, skip rootId={}", rootQuestionId);
                return;
            }

            var chain = collectChainFromRoot(root);
            boolean allAnswered = chain.stream().allMatch(p -> p.a() != null && !p.a().isBlank());
            if (!allAnswered) {
                log.info("[feedback] some answers are blank, skip rootId={}", rootQuestionId);
                return;
            }

            String aiFeedback = sttFeedbackService.generateAiFeedbackForRoot(chain);
            if (aiFeedback == null || aiFeedback.isBlank()) {
                log.warn("[feedback] empty feedback from LLM, rootId={}", rootQuestionId);
                return;
            }

            root.attachAiFeedback(aiFeedback);

            log.info("[feedback] saved, rootId={}", rootQuestionId);

        } catch (Exception e) {
            log.error("[feedback] async job failed, rootId={}", rootQuestionId, e);
            // 재시도 큐
        }
    }

    private List<QAPair> collectChainFromRoot(InterviewQuestion root) {
        List<QAPair> qa = new ArrayList<>();
        InterviewQuestion cur = root;
        int depth = 0;
        while (cur != null) {
            if (++depth > 100) throw new ApiException(ErrorCode.DATA_INTEGRITY_VIOLATED);
            String qText = cur.getQuestion() == null ? "" : cur.getQuestion();
            String aText = (cur.getRecording() != null && cur.getRecording().getSttText() != null)
                    ? cur.getRecording().getSttText() : "";
            qa.add(new QAPair(qText, aText));
            cur = cur.getFollowUpQuestion();
        }
        return qa;
    }

    public record QAPair(String q, String a) {}
}
