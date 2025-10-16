package com.dgu.review.domain.interview.service;

import com.dgu.review.domain.interview.dto.response.GetRandomQuestionResponse;
import com.dgu.review.domain.interview.entity.InterviewQuestion;
import com.dgu.review.domain.peerFeedback.entity.PeerFeedback;
import com.dgu.review.domain.peerFeedback.repository.PeerFeedbackRepository;
import com.dgu.review.global.exception.ApiException;
import com.dgu.review.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class RandomQuestionService {

    private final PeerFeedbackRepository peerFeedbackRepository;

    public RandomQuestionService(PeerFeedbackRepository peerFeedbackRepository) {
        this.peerFeedbackRepository = peerFeedbackRepository;
    }

    public GetRandomQuestionResponse getRandomQuestion(Long peerAnswerId) {
        PeerFeedback peerFeedback = peerFeedbackRepository.findById(peerAnswerId)
                .orElseThrow(() -> new ApiException(ErrorCode.PEER_FEEDBACK_NOT_FOUND));

        InterviewQuestion interviewQuestion = InterviewQuestion.builder()
                //.question(peerFeedback.getFollowUpQuestion) 타인평가에 추가된 필드
                .interviewSession(peerFeedback.getRecording().getInterviewQuestion().getInterviewSession())
                .parentQuestion(peerFeedback.getRecording().getInterviewQuestion())
                .sourcePeerFeedback(peerFeedback)
                .build();



        GetRandomQuestionResponse.Question question = new GetRandomQuestionResponse.Question(
                interviewQuestion.getId(),
                interviewQuestion.getQuestion()
        );

        GetRandomQuestionResponse.Context context = new GetRandomQuestionResponse.Context(
                peerFeedback.getRecording().getInterviewQuestion().getId(),
                peerFeedback.getRecording().getInterviewQuestion().getQuestion(),
                peerFeedback.getRecording().getObjectKey(),
                peerFeedback.getRecording().getSttText()
        );

        return new GetRandomQuestionResponse(question, context);
    }

}
