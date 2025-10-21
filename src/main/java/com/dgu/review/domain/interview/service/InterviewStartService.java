package com.dgu.review.domain.interview.service;

import com.dgu.review.domain.interview.dto.InterviewCreateRequest;
import com.dgu.review.domain.interview.dto.response.ExtractedResume;
import com.dgu.review.domain.interview.dto.response.StartInterviewResponse;
import com.dgu.review.domain.interview.entity.InterviewQuestion;
import com.dgu.review.domain.interview.entity.InterviewSession;
import com.dgu.review.domain.interview.repository.InterviewQuestionRepository;
import com.dgu.review.domain.interview.repository.InterviewSessionRepository;
import com.dgu.review.domain.user.entity.User;
import com.dgu.review.domain.user.repository.UserRepository;
import com.dgu.review.domain.user.service.GetUserService;
import com.dgu.review.global.exception.ApiException;
import com.dgu.review.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class InterviewStartService {

    private final InterviewPreparationService interviewPreparationService;
    private final QuestionCreator questionCreator;
    private final InterviewQuestionRepository interviewQuestionRepository;
    private final UserRepository userRepository;
    private final InterviewSessionRepository interviewSessionRepository;
    private final GetUserService getUserService;

    public StartInterviewResponse startInterview(InterviewCreateRequest req) {

        // 자소서 변환
        ExtractedResume resume = interviewPreparationService.extract(req);
        log.info("자소서 텍스트:{}",resume.resumeText());

        Long userId = getUserService.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        InterviewSession session = interviewSessionRepository.save(InterviewSession.builder()
                .title(java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")))
                .resumeObjectKey(resume.resumeObjectKey())
                .mode(req.mode())
                .jobRole(req.jobRole())
                .user(user)
                .build());

        List<String> questionTexts = questionCreator.createInterviewQuestions(
                req.mode().name(), req.jobRole(), resume.resumeText());

        if (questionTexts == null || questionTexts.isEmpty()) {
            throw new ApiException(ErrorCode.LLM_RESPONSE_EMPTY);
        }

        List<InterviewQuestion> savedQuestions =
                IntStream.range(0, questionTexts.size())
                        .mapToObj(i -> InterviewQuestion.builder()
                                        .questionNumber(i + 1)
                                        .question(questionTexts.get(i))
                                        .interviewSession(session)
                                        .build()
                        ).map(interviewQuestionRepository::save)
                        .toList();

        return new StartInterviewResponse(
                session.getId(),
                savedQuestions.get(0).getId(),
                savedQuestions.get(0).getQuestion()
        );


    }
}
