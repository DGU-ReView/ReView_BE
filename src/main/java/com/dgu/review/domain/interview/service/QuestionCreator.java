package com.dgu.review.domain.interview.service;

import com.dgu.review.domain.interview.entity.InterviewMode;
import com.dgu.review.global.exception.ApiException;
import com.dgu.review.global.exception.ErrorCode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class QuestionCreator {

    private final BedrockRuntimeClient bedrockRuntimeClient;
    private final ObjectMapper objectMapper;

    @Value("${bedrock.model-id}")
    private String modelId;

    public List<String> createInterviewQuestions(String interviewMode, String jobRole, String resumeText) {

        var systemBlocks = List.of(
                SystemContentBlock.fromText("""
                        너는 전문 면접관 AI이며, 지원자의 자소서와 희망 직군을 분석해 실제 기업 면접에서 사용할 수 있는 질문을 작성한다. \s
                        질문은 지원자의 경험과 직무 역량을 깊이 있게 탐색하도록 구성하며, 압박면접 여부에 따라 어조를 조정한다.
                        """
                ));

        String userPayload = """
                        [입력 정보]
                        - 면접 난이도: %s   # normal 또는 hard
                        - 희망 직군: %s
                        
                        [자소서]
                        %s
                        
                        [요청]
                        - 총 4개의 질문을 생성한다.
                        - 모든 질문은 자소서의 구체적인 경험이나 서술을 토대로 해야 하며, 표면적 요약이나 단순 반복은 금지한다.
                        - 질문 간에는 탐색 방향이 겹치지 않도록 다양하게 구성한다.
                        - 면접 난이도가 hard이면 직설적이고 논리적 압박이 느껴지는 질문으로 작성한다.
                        - 면접 난이도가 normal이면 대화형이지만 분석적 깊이를 갖춘 질문으로 작성한다.
                        - 각 질문은 "면접관이 실제로 말할 법한 자연스러운 한국어 문장"으로 표현한다.
                        - 추가로, 각 질문 아래에 그 질문의 의도(면접 평가 포인트)를 간결히 작성한다.
                        - 출력은 JSON 배열로, 각 원소는 아래 필드를 가진다.
                            - questionNumber: 1..4
                            - questionText: "면접관이 실제로 말할 법한 자연스러운 한국어 질문"
                            - intent: "이 질문으로 평가하려는 핵심(예: 직무이해/문제해결/협업 등)"
                        - 기업명/인명/개인정보는 넣지 않는다.
                        
                        [출력 조건]
                        - JSON 형식을 반드시 준수하되, 한국어 문자열 안의 따옴표와 개행 오류가 없도록 주의한다.
                        """.formatted(
                                nullSafeLower(interviewMode), nullSafe(jobRole), nullSafe(resumeText)
                // 강조하고 싶은 부분 역량, 부족한 역량 부분 프롬프트에 추가
        );

        var userMsg = Message.builder()
                .content(List.of(ContentBlock.fromText(userPayload)))
                .role(ConversationRole.USER)
                .build();

        ConverseResponse resp = bedrockRuntimeClient.converse(
                ConverseRequest.builder()
                        .modelId(modelId)
                        .system(systemBlocks)
                        .messages(List.of(userMsg))
                        .inferenceConfig(config -> config
                                .maxTokens(800)
                                .temperature(0.4f)
                        ).build()
        );

        // 텍스트 추출
        String raw = resp.output().message().content().isEmpty()
                ? ""
                : resp.output().message().content().get(0).text();

        // JSON 파싱
        List<Map<String, Object>> items = null;

        try {
            items = objectMapper.readValue(raw, new TypeReference<>() {});
        } catch(Exception e) {
            throw new ApiException(ErrorCode.LLM_INTERNAL_ERROR);
        }

        List<String> questions = new ArrayList<>();
        for (Map<String, Object> it : items) {
            Object qt = it.get("questionText");
            if (qt instanceof String s && !s.isBlank()) {
                questions.add(s.trim());
            }
        }

        return questions;
    }

    private String nullSafe(String s) {
        return s == null ? "" : s;
    }

    private String nullSafeLower(String s) {
        return s == null ? "" : s.toLowerCase();
    }
}
