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
    private final PeerFeedbackRepository peerFeedbackRepository;
    private final UserRepository userRepository;
    private final InterviewQuestionRepository interviewQuestionRepository;
    private final RecordingRepository recordingRepository;

    @Value("${bedrock.model-id}")
    private String modelId;

    public String generateAiFeedback(Long sessionId, Long questionId) {

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

    //아래 주석은 피드백 생성부
//    // ai 호출부 분리
//    // 다른
//    //응답, 함수이름, 요청 바꾸기 api 명세서나 피그마 참고해서
//    public SttFeedbackResponse generateAiFeedback(Long sessionId, Long questionId) {
//
//        //Recording recording =
//        InterviewQuestion interviewQuestion = interviewQuestionRepository.findById(questionId)
//                .orElseThrow(() -> new ApiException(ErrorCode.INTERVIEW_QUESTION_NOT_FOUND));
//        String questionText = interviewQuestion.getQuestion();
//        String sttText = interviewQuestion.getRecording().getSttText();
//        //자소서
//
//        Message systemMsg = Message.builder()
//                .role(ConversationRole.fromValue("system"))
//                .content(ContentBlock.fromText(
//                        """
//                        너는 모의면접 코치다. 다음 원칙을 지켜서 "한국어 줄글"로 피드백을 작성하라.
//
//                        - 톤: 전문적이되 친절하고 구체적. 불필요한 사족, 과도한 사과/면책 표현 금지.
//                        - 길이: 8~12문장. (짧게 원하면 4~6문장, 길게 원하면 12~16문장)
//                        - 구조(줄글, 소제목 없이 문단으로):
//                          1) 질문의 의도와 핵심 평가 포인트를 먼저 간결히 짚는다.
//                          2) 답변의 강점을 2~3가지 구체적 근거와 함께 설명한다.
//                          3) 개선이 필요한 부분을 2~3가지 지적하고, 각 항목마다 바로 적용 가능한 수정 예시를 제시한다.
//                          4) 마지막 문장에 한 줄 코칭 포인트로 마무리한다.
//                        - 금지: JSON, 목록기호(•, -, 1.), 표, 코드블록, 과장/추측.
//
//                        필요 시 후보자 프로필(자소서 요약)을 맥락으로 참고하되, 원문 인용은 삼가고 핵심만 녹여서 평가하라.
//                        """
//                )).build();
//
//        var userMsg = Message.builder()
//                .content(ContentBlock.fromText("면접 질문: " + questionText))
//                .content(ContentBlock.fromText("지원자 답변(STT): " + sttText))
//                .role(ConversationRole.USER)
//                .build();
//
//        ConverseResponse resp = bedrockRuntimeClient.converse(
//                ConverseRequest.builder()
//                        .modelId(modelId)
//                        .messages(List.of(systemMsg, userMsg))
//                        .inferenceConfig(config -> config
//                                .maxTokens(maxTokens)
//                                .temperature(temperature.floatValue())
//                                .topP(topP.floatValue())
//                        ).build()
//        );
//
//        String respText = "";
//        var out = resp.output();
//        if (out != null && out.message() != null && !out.message().content().isEmpty()) {
//            var firstBlock = out.message().content().get(0);
//            respText = firstBlock != null ? firstBlock.text() : "";
//        }
//
//        //답변이 너무 짧거나 토큰 사용량을 확인하고 싶으면 로깅 추가
//
//        var usage = resp.usage();
//        int promptTokens = usage != null ? usage.inputTokens() : 0;
//        int completionTokens = usage != null ? usage.outputTokens() : 0;
//
//        String stopReason = resp.stopReasonAsString();
//        boolean truncated = "length".equalsIgnoreCase(stopReason);
//
//        return SttFeedbackResponse.builder()
//                .feedback(respText)
//                .modelId(modelId)
//                .promptTokens(promptTokens)
//                .completionTokens(completionTokens)
//                .stopReason(stopReason)
//                .truncated(truncated)
//                .build();
//
//    }
//
//    public SttFeedbackResponse generateSelfFeedback() {
//
//        // 질문과 응답 text를 넘겨주는 로직을 추후 하나로 통일해서 이미 꺼내온 stt 텍스트를 주는 방향으로
//        String questionText = "본인의 강점을 구체적 사례와 함께 설명해주세요.";
//        String sttText = "저의 강점은 장애 대응과 성능 최적화입니다. 최근 프로젝트에서...";
//        //자소서
//
//        User user = userRepository.findById(1L)
//                .orElseThrow(IllegalArgumentException::new); // 예외처리 로직 수정
//
//        List<PeerFeedback> myPeerFeedbacks = peerFeedbackRepository.findAllByUser(user);
//
//
//        Message systemMsg = Message.builder()
//                .role(ConversationRole.fromValue("system"))
//                .content(ContentBlock.fromText(
//                        """
//                        너는 모의면접 코치다. 다음 원칙을 지켜서 "한국어 줄글"로 피드백을 작성하라.
//
//                        - 톤: 전문적이되 친절하고 구체적. 불필요한 사족, 과도한 사과/면책 표현 금지.
//                        - 길이: 8~12문장. (짧게 원하면 4~6문장, 길게 원하면 12~16문장)
//                        - 구조(줄글, 소제목 없이 문단으로):
//                          1) 질문의 의도와 핵심 평가 포인트를 먼저 간결히 짚는다.
//                          2) 답변의 강점을 2~3가지 구체적 근거와 함께 설명한다.
//                          3) 개선이 필요한 부분을 2~3가지 지적하고, 각 항목마다 바로 적용 가능한 수정 예시를 제시한다.
//                          4) 마지막 문장에 한 줄 코칭 포인트로 마무리한다.
//                        - 금지: JSON, 목록기호(•, -, 1.), 표, 코드블록, 과장/추측.
//
//                        필요 시 후보자 프로필(자소서 요약)을 맥락으로 참고하되, 원문 인용은 삼가고 핵심만 녹여서 평가하라.
//                        """
//                )).build();
//
//        var userMsg = Message.builder()
//                .content(ContentBlock.fromText("면접 질문: " + questionText))
//                .content(ContentBlock.fromText("지원자 답변(STT): " + sttText))
//                .role(ConversationRole.USER)
//                .build();
//
//        ConverseResponse resp = bedrockRuntimeClient.converse(
//                ConverseRequest.builder()
//                        .modelId(modelId)
//                        .messages(List.of(systemMsg, userMsg))
//                        .inferenceConfig(config -> config
//                                .maxTokens(maxTokens)
//                                .temperature(temperature.floatValue())
//                                .topP(topP.floatValue())
//                        ).build()
//        );
//
//        String respText = "";
//        var out = resp.output();
//        if (out != null && out.message() != null && !out.message().content().isEmpty()) {
//            var firstBlock = out.message().content().get(0);
//            respText = firstBlock != null ? firstBlock.text() : "";
//        }
//
//        //답변이 너무 짧거나 토큰 사용량을 확인하고 싶으면 로깅 추가
//
//        var usage = resp.usage();
//        int promptTokens = usage != null ? usage.inputTokens() : 0;
//        int completionTokens = usage != null ? usage.outputTokens() : 0;
//
//        String stopReason = resp.stopReasonAsString();
//        boolean truncated = "length".equalsIgnoreCase(stopReason);
//
//        return SttFeedbackResponse.builder()
//                .feedback(respText)
//                .modelId(modelId)
//                .promptTokens(promptTokens)
//                .completionTokens(completionTokens)
//                .stopReason(stopReason)
//                .truncated(truncated)
//                .build();

//    }
}
