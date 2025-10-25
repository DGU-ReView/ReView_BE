package com.dgu.review.domain.interview.service;

import com.dgu.review.domain.interview.entity.InterviewQuestion;
import com.dgu.review.domain.interview.repository.InterviewQuestionRepository;
import com.dgu.review.global.exception.ApiException;
import com.dgu.review.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.*;
import com.google.common.util.concurrent.RateLimiter;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class SttFeedbackService {

    private final RateLimiter tokenLimiter;
    private final BedrockRuntimeClient bedrockRuntimeClient;
    private final InterviewQuestionRepository interviewQuestionRepository;
    private final RateLimiter bedrockLimiter = RateLimiter.create(0.3);

    @Value("${bedrock.model-id}")
    private String modelId;

    public String generateAiFollowUp(Long questionId) {

        InterviewQuestion interviewQuestion = interviewQuestionRepository.findById(questionId)
                .orElseThrow(() -> new ApiException(ErrorCode.INTERVIEW_QUESTION_NOT_FOUND));
        String questionText = interviewQuestion.getQuestion();
        String sttText = interviewQuestion.getRecording().getSttText();
        //자소서

        if (sttText == null || sttText.isBlank()) {
            log.info("[followup] skip generation due to empty STT, qId={}", questionId);
            return "추가 질문이 필요하지 않습니다.";
        }

        var systemBlocks = List.of(
                SystemContentBlock.fromText("""
                       너는 모의면접 코치다. 다음 원칙을 지켜서 "한국어 한 문장"으로 꼬리질문을 생성하라.
                
                       - 목적: 후보자의 답변이 충분하지 않거나 구체성이 부족한 부분을 보완하기 위해 추가 질문을 한다.
                       - 톤: 면접 상황에 맞게 전문적이되 자연스럽고 간결해야 한다.
                       - 출력 형식: 문장 1개만 출력하며, JSON, 불릿, 번호, 설명, 사족 없이 질문 문장만 제시한다.
                       - 규칙:
                         1) 답변이 이미 충분하고 더 묻지 않아도 될 경우, "추가 질문이 필요하지 않습니다."라고 정확히 출력한다.
                         2) 답변의 핵심이 명확하고 논리적으로 완결되어 있다면, 사소한 세부사항 부족은 굳이 보완하지 않는다.
                         3) 부족하거나 빠진 내용이 있다면 그 부분을 한 문장 꼬리질문으로 물어본다.
                         4) 이미 언급된 내용을 표현만 바꿔 되묻거나, 지나치게 캐묻는 질문은 생성하지 않는다.
                         5) 질문은 열린 질문으로, 답변자가 추가 설명을 유도할 수 있게 작성한다.
                         6) 지나치게 장황하지 말고, 구체적인 정보를 끌어낼 수 있도록 초점을 맞춘다.
                         7) 답변이 전반적으로 충실하다면 억지로 부족한 점을 찾아내려 하지 말고, 자연스럽게 다음 주제로 넘어갈 줄 알아야 한다.
                         8) 특정 단어(Git, 리더십, 협업, 회의, 문제 해결 등)가 언급되었다는 이유만으로 그 내용을 더 파고드는 질문은 하지 않는다.
                         9) 답변 내에서 ‘회의를 통해 결정했다’, ‘팀 합의를 이끌었다’, ‘논의 후 정했다’ 등의 표현이 있다면, 의견 수렴 및 소통 과정이 이미 포함된 것으로 간주한다.
                       """
                ));

        var userMsg = Message.builder()
                .content(ContentBlock.fromText("면접 질문: " + questionText))
                .content(ContentBlock.fromText("지원자 답변(STT): " + sttText))
                .role(ConversationRole.USER)
                .build();

        ConverseRequest req = ConverseRequest.builder()
                .modelId(modelId)
                .system(systemBlocks)
                .messages(List.of(userMsg))
                .inferenceConfig(c -> c.maxTokens(120)
                        .temperature(0.5f)
                        .topP(0.8f))
                .build();

        ConverseResponse resp = callBedrockWithBackoff(
                () -> bedrockRuntimeClient.converse(req),
                "FOLLOW_UP",
                interviewQuestion.getId()
        );

        String respText = "";
        var out = resp.output();
        if (out != null && out.message() != null && !out.message().content().isEmpty()) {
            var firstBlock = out.message().content().get(0);
            respText = firstBlock != null ? firstBlock.text() : "";
        }

        var usage = resp.usage();
        int promptTokens = usage != null ? usage.inputTokens() : 0;
        int completionTokens = usage != null ? usage.outputTokens() : 0;

        String stopReason = resp.stopReasonAsString();
        boolean truncated = "length".equalsIgnoreCase(stopReason);

        log.info("[Bedrock] questionId={} promptTokens={} completionTokens={} stopReason={} truncated={}",
                questionId, promptTokens, completionTokens, stopReason, truncated);

        return respText;

    }

    public String generateAiFeedbackForRoot(List<FeedbackJob.QAPair> chain, Long rootId) {

        //자소서
        StringBuilder sb = new StringBuilder(9000);
        int totalLimit = 8000;

        for(int i = 0; i < chain.size(); i++) {
            var p = chain.get(i);
//            sb.append("Q").append(i+1).append(": ").append(p.q()).append("\n");
//            sb.append("A").append(i+1).append("(STT): ").append(p.a()).append("\n");

            String q = limit(p.q(), 800);
            String a = limit(p.a(), 2000);
            String chunk = "Q" + (i + 1) + ": " + q + "\n" + "A" + (i + 1) + "(STT): " + a + "\n";
            if (sb.length() + chunk.length() > totalLimit) break;
            sb.append(chunk);
        }

        var systemBlocks = List.of(
                SystemContentBlock.fromText("""
                        너는 모의면접 코치다. 아래 선형 Q/A 체인을 종합해 다음 원칙을 지켜서 "한국어 줄글"로 피드백을 작성하라.
                        
                        [최우선 절대 규칙: STT 오류 지적 절대 금지]
                        - 입력되는 답변(A1, A2...)은, 사용자가 타자로 직접 친 값이 아닌, 사용자의 음성을 STT 변환 모델로 텍스트로 변환한 결과이다. 이 과정에서 오류(예: '공룡 API' -> '공통 API', '긴 브렌치' -> '깃 브랜치')가 포함될 수 있다.
                        - 너는 STT(음성인식) 변환 과정에서 생긴 오류를 절대 지적해서는 안 된다.
                        - 이는 사용자의 오타나 발음 실수가 아닌, 100% STT 모델의 오류이다.
                        - STT 오류를 사용자의 '오타', '발음', '부정확한 표현' 등으로 해석하거나 지적하는 것을 엄격히 금지한다.
                        - 절대로 이러한 STT 변환 오류, 오타, 또는 부정확한 발음을 지적하거나 피드백 대상으로 삼지 마라.
                        - 금지 단어: "오타", "발음" (STT 오류를 지칭할 때 사용 금지)
                        - 대신, 항상 발언의 의도와 맥락을 추론하여, 답변의 내용, 논리, 구조, 설득력 등 본질적인 부분에만 집중하여 피드백하라.
                        - 이 규칙은 아래의 모든 지시사항보다 우선한다.

                        - 톤: 전문적이되 친절하고 구체적. 불필요한 사족, 과도한 사과/면책 표현 금지.
                        - 길이: 4~5문장
                        - 구조(줄글, 소제목 없이 문단으로):
                          1) 질문의 의도와 핵심 평가 포인트를 먼저 간결히 짚는다.
                          2) 답변의 강점을 1~2가지 구체적 근거와 함께 설명한다.
                          3) 개선이 필요한 부분을 1~2가지 지적하고, 각 항목마다 바로 적용 가능한 수정 예시를 제시한다.
                          4) 마지막 문장에 한 줄 코칭 포인트로 마무리한다.
                          5) 모든 꼬리질문까지의 누적 맥락을 일관되게 평가한다.
                        - 금지: JSON, 목록기호(•, -, 1.), 표, 코드블록, 과장/추측.

                        필요 시 후보자 프로필(자소서 요약)을 맥락으로 참고하되, 원문 인용은 삼가고 핵심만 녹여서 평가하라.
                        """
                ));

        // 강조하고싶은 역량, 부족한 역량 부분 프롬프트에 추가

        var userMsg = Message.builder()
                .content(ContentBlock.fromText(sb.toString()))
                .role(ConversationRole.USER)
                .build();

        ConverseRequest req = ConverseRequest.builder()
                .modelId(modelId)
                .system(systemBlocks)
                .messages(List.of(userMsg))
                .inferenceConfig(c -> c.maxTokens(500)
                        .temperature(0.5f)
                        .topP(0.8f))
                .build();

        acquireTokensBudget(req);
        ConverseResponse resp = callBedrockWithBackoff(
                () -> bedrockRuntimeClient.converse(req),
                "AI_FEEDBACK",
               rootId
        );

        String respText = "";
        var out = resp.output();
        if (out != null && out.message() != null && !out.message().content().isEmpty()) {
            var firstBlock = out.message().content().get(0);
            respText = firstBlock != null ? firstBlock.text() : "";
        }

        var usage = resp.usage();
        int promptTokens = usage != null ? usage.inputTokens() : 0;
        int completionTokens = usage != null ? usage.outputTokens() : 0;

        String stopReason = resp.stopReasonAsString();
        boolean truncated = "length".equalsIgnoreCase(stopReason);

        log.info("[Bedrock] questionId={} promptTokens={} completionTokens={} stopReason={} truncated={}",
                rootId, promptTokens, completionTokens, stopReason, truncated);

        return respText;

    }

    public String generateSelfFeedbackForRoot(List<FeedbackJob.QAPair> chain, List<String> myPeerFeedbacks, Long rootId) {

        //자소서

        StringBuilder sb = new StringBuilder(10000);

        if (myPeerFeedbacks != null && !myPeerFeedbacks.isEmpty()) {
            sb.append("\n[사용자가 과거에 다른 지원자에게 남긴 피드백 예시]\n");
            int idx = 1, used = 0;
            for (String s : myPeerFeedbacks) {
                if (s == null || s.isBlank()) continue;
                sb.append(idx++).append(") ").append(limit(s, 800)).append("\n\n");
                if (++used >= 5) break;
            }
        }

        sb.append("[이번 면접 Q/A 체인]\n");
        int totalLimit = 8000;
        for(int i = 0; i < chain.size(); i++) {
            var p = chain.get(i);
//            sb.append("Q").append(i+1).append(": ").append(p.q()).append("\n");
//            sb.append("A").append(i+1).append("(STT): ").append(p.a()).append("\n\n");
            String q = limit(p.q(), 800);
            String a = limit(p.a(), 2000);
            String chunk = "Q" + (i + 1) + ": " + q + "\n" +
                    "A" + (i + 1) + "(STT): " + a + "\n\n";
            if (sb.length() + chunk.length() > totalLimit) break;
            sb.append(chunk);
        }

        var systemBlocks = List.of(
                SystemContentBlock.fromText("""
                        너는 '모의면접 피어리뷰어'이며, 아래 입력에는 두 가지 정보가 포함되어 있다.
                        1. 사용자가 과거 다른 지원자에게 남긴 피드백 예시 (피어리뷰 스타일 데이터)
                        2. 사용자의 이번 면접 Q/A 체인(STT 전사 포함)

                        이 정보를 바탕으로, "그 사용자가 자기 자신에게 피드백을 남긴다면"
                        어떤 식으로 쓸지 최대한 자연스럽고 일관된 문체로 '한국어 줄글' 피드백을 작성하라.
                        
                        [최우선 절대 규칙: STT 오류 지적 절대 금지]
                        - '오타'라는 단어를 사용하지 마라.
                        - 입력되는 답변(A1, A2...)은, 사용자가 타자로 직접 친 값이 아닌, 사용자의 음성을 STT 변환 모델로 텍스트로 변환한 결과이다. 이 과정에서 오류(예: '공룡 API' -> '공통 API', '긴 브렌치' -> '깃 브랜치')가 포함될 수 있다.
                        - 너는 STT(음성인식) 변환 과정에서 생긴 오류를 절대 지적해서는 안 된다.
                        - 이는 사용자의 오타나 발음 실수가 아닌, 100% STT 모델의 오류이다.
                        - STT 오류를 사용자의 '오타', '발음', '부정확한 표현' 등으로 해석하거나 지적하는 것을 엄격히 금지한다.
                        - [사용자가 과거에 다른 지원자에게 남긴 피드백 예시] 데이터에 STT 오류나 발음을 지적하는 내용이 포함되어 있더라도, 그 부분은 절대로 모방해서는 안 된다.
                        - 절대로 이러한 STT 변환 오류, 오타, 또는 부정확한 발음을 지적하거나 피드백 대상으로 삼지 마라.
                        - 금지 단어: "오타", "발음" (STT 오류를 지칭할 때 사용 금지)
                        - 대신, 항상 발언의 의도와 맥락을 추론하여, 답변의 내용, 논리, 구조, 설득력 등 본질적인 부분에만 집중하여 피드백하라.
                        - 이 규칙은 아래의 모든 지시사항보다 우선한다.

                        [역할 및 목표]
                        - 너는 AI 코치가 아니라, 사용자의 피어리뷰 스타일을 재현하는 대리인이다.
                        - 사용자의 피드백 관점(강조하는 포인트, 톤, 비판 방식, 표현 습관)을 적극 반영하라.
                        - 단, 자기 합리화나 과도한 칭찬으로 흐르지 않게 균형을 유지한다.
                        - 스스로를 평가하는 만큼, 구체적 근거와 개선 의지를 드러내는 방향으로 쓴다.
                        
                        [작성 규칙]
                        - 형식: 줄글 (JSON, 목록, 불릿, 번호, 소제목, 표, 코드블록 모두 금지)
                        - 분량: 4~5문장
                        - 문체: 피드백 예시에서 관찰된 어조와 구조를 따라라.
                          (예: 칭찬 후 개선점 구조, '~점이 좋았습니다' '~보완하면 더 좋겠습니다' 식)
                        - 내용 구조:
                          1) 면접 답변의 전체적인 인상과 주제 적합성 평가
                          2) 답변의 강점 1~2가지 (구체적 사례나 표현)
                          3) 개선점 1~2가지 (실행 가능한 조언 중심)
                          4) 스스로에게 주는 코칭 한 줄로 마무리
                        - 평가 기준은 AI의 기준이 아니라, 입력된 피어리뷰 예시에서 드러난 가치관과 판단기준을 따른다.
                
                        [추가 지침]
                        - 피어리뷰 예시는 "학습 데이터"로 사용하되, 그 문장을 그대로 복사하거나 반복하지 말고
                          톤·관점·문체를 참고하여 새로운 내용으로 작성한다.
                        - 자소서나 답변 내용이 충분히 명확하면 불필요한 반복 피드백은 피한다.
                        - 자기 피드백답게 "내가 다음에는 어떻게 할지"로 끝맺으면 좋다.
                        """
                ));

        // 강조하고싶은 역량, 부족한 역량 부분 프롬프트에 추가 - 셀프 피드백은 x

        var userMsg = Message.builder()
                .content(ContentBlock.fromText(sb.toString()))
                .role(ConversationRole.USER)
                .build();

        ConverseRequest req = ConverseRequest.builder()
                .modelId(modelId)
                .system(systemBlocks)
                .messages(List.of(userMsg))
                .inferenceConfig(c -> c.maxTokens(500)
                        .temperature(0.5f)
                        .topP(0.8f))
                .build();

        acquireTokensBudget(req);
        ConverseResponse resp = callBedrockWithBackoff(
                () -> bedrockRuntimeClient.converse(req),
                "SELF_FEEDBACK",
                rootId
        );

        String respText = "";
        var out = resp.output();
        if (out != null && out.message() != null && !out.message().content().isEmpty()) {
            var firstBlock = out.message().content().get(0);
            respText = firstBlock != null ? firstBlock.text() : "";
        }

        var usage = resp.usage();
        int promptTokens = usage != null ? usage.inputTokens() : 0;
        int completionTokens = usage != null ? usage.outputTokens() : 0;

        String stopReason = resp.stopReasonAsString();
        boolean truncated = "length".equalsIgnoreCase(stopReason);

        log.info("[Bedrock] questionId={} promptTokens={} completionTokens={} stopReason={} truncated={}",
                rootId, promptTokens, completionTokens, stopReason, truncated);

        return respText;

    }

    /** 너무 긴 텍스트를 잘라서 토큰 폭주/429를 완화 (문자 기준, 한글 1~2토큰 추정) */
    private String limit(String s, int maxChars) {
        if (s == null) return "";
        s = s.trim();
        if (s.length() <= maxChars) return s;
        return s.substring(0, maxChars);
    }

    /** Bedrock 호출을 지수 백오프 + 지터로 감싼다. */
    private <T> T callBedrockWithBackoff(Supplier<T> fn, String purpose, long rootId) {
        double waited = bedrockLimiter.acquire();
        if (waited > 0) {
            log.debug("[bedrock] rate-limiter waited {}s for {}", String.format("%.2f", waited), purpose);
        }

        int max = 10;
        long base = 600L;
        final long cap  = 30_000L;
        for (int i = 1; i <= max; i++) {
            try {
                return fn.get();
            } catch (ThrottlingException te) {
                long jitter = ThreadLocalRandom.current().nextLong(200, 800);
                long backoff = Math.min((long) (base * Math.pow(2, i - 1)) + jitter, cap);
                log.warn("[bedrock] 429 throttle ({}). retry {}/{} in {}ms, rootId={}", purpose, i, max, backoff, rootId);
                sleepQuiet(backoff);
            } catch (SdkClientException sce) {
                long jitter  = ThreadLocalRandom.current().nextLong(0, 600);
                long backoff = Math.min(700L * i + jitter, cap);
                log.warn("[bedrock] SdkClientException. retry {}/{} in {}ms, rootId={}, msg={}",
                        i, max, backoff, rootId, sce.getMessage());
                sleepQuiet(backoff);
            }
        }
        throw new RuntimeException("Bedrock throttled/exhausted: " + purpose);
    }

    private void sleepQuiet(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }

    private int estimateTokens(String s) {
        if (s == null) return 0;
        int chars = s.length();
        return (int)Math.ceil(chars * 1.6);
    }

    private void acquireTokensBudget(ConverseRequest req) {
        int approx = 0;
        if (req.system() != null) {
            for (var b : req.system()) approx += estimateTokens(b.text());
        }
        if (req.messages() != null) {
            for (var m : req.messages()) {
                for (var c : m.content()) approx += estimateTokens(c.text());
            }
        }
        var max = req.inferenceConfig().maxTokens() == null ? 500 : req.inferenceConfig().maxTokens();
        approx += max;

        double waited = tokenLimiter.acquire(approx);
        if (waited > 0) log.debug("[bedrock] tokenLimiter waited {}s (~{} tokens)", String.format("%.2f", waited), approx);
    }

}
