package com.dgu.review.domain.interview.service;

import com.dgu.review.domain.interview.dto.response.GetFollowUpQuestionResponse;
import com.dgu.review.domain.interview.entity.InterviewQuestion;
import com.dgu.review.domain.interview.repository.InterviewQuestionRepository;
import com.dgu.review.domain.interview.repository.RecordingRepository;
import com.dgu.review.domain.peerFeedback.repository.PeerFeedbackRepository;
import com.dgu.review.domain.user.repository.UserRepository;
import com.dgu.review.global.exception.ApiException;
import com.dgu.review.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.*;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SttFeedbackService {

    private final BedrockRuntimeClient bedrockRuntimeClient;
    private final InterviewQuestionRepository interviewQuestionRepository;

    @Value("${bedrock.model-id}")
    private String modelId;

    public String generateAiFeedback(Long questionId) {

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
                         2) 부족하거나 빠진 내용이 있다면 그 부분을 한 문장 꼬리질문으로 물어본다.
                         3) 질문은 열린 질문으로, 답변자가 추가 설명을 유도할 수 있게 작성한다.
                         4) 지나치게 장황하지 말고, 구체적인 정보를 끌어낼 수 있도록 초점을 맞춘다.
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

        // 위 값들 로그에 추가

        return respText;

    }


}
