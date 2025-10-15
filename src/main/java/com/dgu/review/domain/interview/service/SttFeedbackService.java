package com.dgu.review.domain.interview.service;

import com.dgu.review.domain.interview.entity.InterviewQuestion;
import com.dgu.review.domain.interview.repository.InterviewQuestionRepository;
import com.dgu.review.global.exception.ApiException;
import com.dgu.review.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.*;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SttFeedbackService {

    private final BedrockRuntimeClient bedrockRuntimeClient;
    private final InterviewQuestionRepository interviewQuestionRepository;

    @Value("${bedrock.model-id}")
    private String modelId;

    public String generateAiFollowUp(Long questionId) {

        InterviewQuestion interviewQuestion = interviewQuestionRepository.findById(questionId)
                .orElseThrow(() -> new ApiException(ErrorCode.INTERVIEW_QUESTION_NOT_FOUND));
        String questionText = interviewQuestion.getQuestion();
        String sttText = interviewQuestion.getRecording().getSttText();
        //자소서

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

        ConverseResponse resp = bedrockRuntimeClient.converse(
                ConverseRequest.builder()
                        .modelId(modelId)
                        .system(systemBlocks)
                        .messages(List.of(userMsg))
                        .inferenceConfig(config -> config
                                .maxTokens(150)
                                .temperature(0.5f)
                                .topP(0.8f)
                        ).build()
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

    public String generateAiFeedbackForRoot(List<FeedbackJob.QAPair> chain) {

//        InterviewQuestion root = interviewQuestionRepository.findById(rootQuestionId)
//                .orElseThrow(() -> new ApiException(ErrorCode.INTERVIEW_QUESTION_NOT_FOUND));
//        String questionText = root.getQuestion();
//        String sttText = interviewQuestion.getRecording().getSttText();
//        //자소서

        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < chain.size(); i++) {
            var p = chain.get(i);
            sb.append("Q").append(i+1).append(": ").append(p.q()).append("\n");
            sb.append("A").append(i+1).append("(STT): ").append(p.a()).append("\n");
        }

        var systemBlocks = List.of(
                SystemContentBlock.fromText("""
                        너는 모의면접 코치다. 아래 선형 Q/A 체인을 종합해 다음 원칙을 지켜서 "한국어 줄글"로 피드백을 작성하라.

                        - 톤: 전문적이되 친절하고 구체적. 불필요한 사족, 과도한 사과/면책 표현 금지.
                        - 길이: 8~12문장
                        - 구조(줄글, 소제목 없이 문단으로):
                          1) 질문의 의도와 핵심 평가 포인트를 먼저 간결히 짚는다.
                          2) 답변의 강점을 2~3가지 구체적 근거와 함께 설명한다.
                          3) 개선이 필요한 부분을 2~3가지 지적하고, 각 항목마다 바로 적용 가능한 수정 예시를 제시한다.
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

        ConverseResponse resp = bedrockRuntimeClient.converse(
                ConverseRequest.builder()
                        .modelId(modelId)
                        .system(systemBlocks)
                        .messages(List.of(userMsg))
                        .inferenceConfig(config -> config
                                .maxTokens(800)
                                .temperature(0.5f)
                                .topP(0.8f)
                        ).build()
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

//        log.info("[Bedrock] questionId={} promptTokens={} completionTokens={} stopReason={} truncated={}",
//                questionId, promptTokens, completionTokens, stopReason, truncated);

        return respText;

    }


}
