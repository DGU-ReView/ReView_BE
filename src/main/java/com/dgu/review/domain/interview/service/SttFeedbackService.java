package com.dgu.review.domain.interview.service;

import com.dgu.review.domain.interview.dto.SttFeedbackResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import com.dgu.review.domain.interview.localTest.FeedbackGenResponse;
import software.amazon.awssdk.services.bedrockruntime.model.*;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SttFeedbackService {

    private final BedrockRuntimeClient bedrockRuntimeClient;

    @Value("${bedrock.model-id}")
    private String modelId;
    @Value("${bedrock.max-tokens}")
    private Integer maxTokens;
    @Value("${bedrock.temperature}")
    private Double temperature;
    @Value("${bedrock.topP}")
    private Double topP;

    //응답, 함수이름, 요청 바꾸기 api 명세서나 피그마 참고해서
    public SttFeedbackResponse generateAiFeedback() {

        // 더미 text로 실행. 추후 개발되는 stt변환 로직에 따라 실제 recordId를 받아오기
        //Recording recording =
        String questionText = "본인의 강점을 구체적 사례와 함께 설명해주세요.";
        String sttText = "저의 강점은 장애 대응과 성능 최적화입니다. 최근 프로젝트에서...";
        //자소서

        Message systemMsg = Message.builder()
                .role(ConversationRole.fromValue("system"))
                .content(ContentBlock.fromText(
                        """
                        너는 모의면접 코치다. 다음 원칙을 지켜서 **한국어 줄글**로 피드백을 작성하라.
                        
                        - 톤: 전문적이되 친절하고 구체적. 불필요한 사족, 과도한 사과/면책 표현 금지.
                        - 길이: 8~12문장. (짧게 원하면 4~6문장, 길게 원하면 12~16문장)
                        - 구조(줄글, 소제목 없이 문단으로):
                          1) 질문의 의도와 핵심 평가 포인트를 먼저 간결히 짚는다.
                          2) 답변의 강점을 2~3가지 구체적 근거와 함께 설명한다.
                          3) 개선이 필요한 부분을 2~3가지 지적하고, 각 항목마다 바로 적용 가능한 수정 예시를 제시한다.
                          4) 마지막 문장에 한 줄 코칭 포인트로 마무리한다.
                        - 금지: JSON, 목록기호(•, -, 1.), 표, 코드블록, 과장/추측.
                        
                        필요 시 후보자 프로필(자소서 요약)을 맥락으로 참고하되, 원문 인용은 삼가고 핵심만 녹여서 평가하라.
                        """
                )).build();

        var userMsg = Message.builder()
                .content(ContentBlock.fromText("면접 질문: " + questionText))
                .content(ContentBlock.fromText("지원자 답변(STT): " + sttText))
                .role(ConversationRole.USER)
                .build();

        ConverseResponse resp = bedrockRuntimeClient.converse(
                ConverseRequest.builder()
                        .modelId(modelId)
                        .messages(List.of(systemMsg, userMsg))
                        .inferenceConfig(config -> config
                                .maxTokens(maxTokens)
                                .temperature(temperature.floatValue())
                                .topP(topP.floatValue())
                        ).build()
        );

        String respText = "";
        var out = resp.output();
        if (out != null && out.message() != null && !out.message().content().isEmpty()) {
            var firstBlock = out.message().content().get(0);
            respText = firstBlock != null ? firstBlock.text() : "";
        }

        //답변이 너무 짧거나 토큰 사용량을 확인하고 싶으면 로깅 추가

        var usage = resp.usage();
        int promptTokens = usage != null ? usage.inputTokens() : 0;
        int completionTokens = usage != null ? usage.outputTokens() : 0;

        String stopReason = resp.stopReasonAsString();
        boolean truncated = "length".equalsIgnoreCase(stopReason);

        return SttFeedbackResponse.builder()
                .feedback(respText)
                .modelId(modelId)
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .stopReason(stopReason)
                .truncated(truncated)
                .build();

    }
}
