package com.dgu.review.domain.interview.service;

import com.dgu.review.domain.interview.entity.InterviewQuestion;
import com.dgu.review.domain.interview.repository.InterviewQuestionRepository;
import com.dgu.review.domain.peerFeedback.repository.PeerFeedbackRepository;
import com.dgu.review.global.exception.ApiException;
import com.dgu.review.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
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
    private final PeerFeedbackRepository peerFeedbackRepository;

    @Async("llmExecutor")
    @Transactional
    public void generateAiThenSelfAsync(Long rootQuestionId) {
        try {
            InterviewQuestion root = interviewQuestionRepository.findById(rootQuestionId)
                    .orElseThrow(() -> new ApiException(ErrorCode.INTERVIEW_QUESTION_NOT_FOUND));

            var chain = collectChainFromRoot(root);
            debugChain("AI", root, chain);

            boolean allAnswered = chain.stream().allMatch(p -> p.a() != null && !p.a().isBlank());
            if (!allAnswered) {
                log.info("[feedback] some answers are blank, skip rootId={}", rootQuestionId);
                return;
            }

            if (root.getAiFeedback() == null || root.getAiFeedback().isBlank()) {
                String aiFeedback = sttFeedbackService.generateAiFeedbackForRoot(chain, rootQuestionId);
                if (aiFeedback == null || aiFeedback.isBlank()) {
                    log.warn("[feedback] empty AI feedback from LLM, rootId={}", rootQuestionId);
                    return;
                }
                root.attachAiFeedback(aiFeedback);
                log.info("[feedback] saved AI, rootId={}", rootQuestionId);
            } else {
                log.info("[feedback] AI already exists, skip rootId={}", rootQuestionId);
            }

            if (root.getSelfFeedback() == null || root.getSelfFeedback().isBlank()) {
                debugChain("SELF", root, chain);
                Long mockUserID = 1L;
                List<String> myPeerFeedbacks =
                        peerFeedbackRepository.findRecentContentsByWriter(mockUserID, PageRequest.of(0, 5));

                String selfFeedback = sttFeedbackService.generateSelfFeedbackForRoot(chain, myPeerFeedbacks, rootQuestionId);
                if (selfFeedback == null || selfFeedback.isBlank()) {
                    log.warn("[feedback] empty SELF feedback from LLM, rootId={}", rootQuestionId);
                    return;
                }
                root.attachSelfFeedback(selfFeedback);
                log.info("[feedback] saved SELF, rootId={}", rootQuestionId);
            } else {
                log.info("[feedback] SELF already exists, skip rootId={}", rootQuestionId);
            }

        } catch (Exception e) {
            log.error("[feedback] async job failed, rootId={}", rootQuestionId, e);
        }
    }


    // 하단 로직은 aiFeedback과 selfFeedback을 병렬로 분리한다면 재사용
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

            debugChain("AI", root, chain);

            boolean allAnswered = chain.stream().allMatch(p -> p.a() != null && !p.a().isBlank());
            if (!allAnswered) {
                log.info("[feedback] some answers are blank, skip rootId={}", rootQuestionId);
                return;
            }

            String aiFeedback = sttFeedbackService.generateAiFeedbackForRoot(chain, rootQuestionId);
            if (aiFeedback == null || aiFeedback.isBlank()) {
                log.warn("[feedback] empty feedback from LLM, rootId={}", rootQuestionId);
                return;
            }

            root.attachAiFeedback(aiFeedback);

            log.info("[feedback] saved, rootId={}", rootQuestionId);


        } catch (Exception e) {
            log.error("[feedback] async job failed, rootId={}", rootQuestionId, e);
        }
    }

    @Async
    @Transactional
    public void generateSelfFeedbackAsync(Long rootQuestionId) {
        try {
            InterviewQuestion root = interviewQuestionRepository.findById(rootQuestionId)
                    .orElseThrow(() -> new ApiException(ErrorCode.INTERVIEW_QUESTION_NOT_FOUND));

            if (root.getSelfFeedback() != null && !root.getSelfFeedback().isBlank()) {
                log.info("[feedback] already exists, skip rootId={}", rootQuestionId);
                return;
            }

            var chain = collectChainFromRoot(root);

            debugChain("SELF", root, chain);

            boolean allAnswered = chain.stream().allMatch(p -> p.a() != null && !p.a().isBlank());
            if (!allAnswered) {
                log.info("[feedback] some answers are blank, skip rootId={}", rootQuestionId);
                return;
            }

            Long mockUserID = 1L;
            List<String> myPeerFeedbacks =
                    peerFeedbackRepository.findRecentContentsByWriter(mockUserID, PageRequest.of(0, 5));

            String selfFeedback = sttFeedbackService.generateSelfFeedbackForRoot(chain, myPeerFeedbacks, rootQuestionId
            );
            if (selfFeedback == null || selfFeedback.isBlank()) {
                log.warn("[feedback] empty feedback from LLM, rootId={}", rootQuestionId);
                return;
            }

            root.attachSelfFeedback(selfFeedback);

            log.info("[feedback] saved, rootId={}", rootQuestionId);

        } catch (Exception e) {
            log.error("[feedback] async job failed, rootId={}", rootQuestionId, e);
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

    private void debugChain(String tag, InterviewQuestion root, List<QAPair> chain) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("[feedback:%s] chain rootId=%d size=%d\n", tag, root.getId(), chain.size()));

            InterviewQuestion cur = root;
            int idx = 0;
            while (cur != null) {
                var rec = (cur.getRecording() == null) ? null : cur.getRecording();
                String qPreview = safePreview(cur.getQuestion(), 40);
                int sttLen = (rec == null || rec.getSttText() == null) ? 0 : rec.getSttText().length();

                sb.append(String.format("  #%d qId=%d parent=%s hasRec=%s sttLen=%d q=\"%s\"\n",
                        idx++,
                        cur.getId(),
                        (cur.getParentQuestion() == null ? "null" : String.valueOf(cur.getParentQuestion().getId())),
                        (rec != null),
                        sttLen,
                        qPreview
                ));
                cur = cur.getFollowUpQuestion();
            }
            log.info(sb.toString());
        } catch (Exception e) {
            log.warn("[feedback:{}] debugChain error", tag, e);
        }
    }

    private String safePreview(String s, int max) {
        if (s == null) return "";
        s = s.replaceAll("\\s+", " ").trim();
        return (s.length() <= max) ? s : s.substring(0, max) + "...";
    }
}
