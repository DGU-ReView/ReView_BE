package com.dgu.review.domain.myarchive.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.dgu.review.domain.interview.entity.InterviewQuestion;
import com.dgu.review.domain.interview.entity.InterviewSession;
import com.dgu.review.domain.interview.entity.Recording;
import com.dgu.review.domain.interview.repository.InterviewQuestionRepository;
import com.dgu.review.domain.interview.repository.InterviewSessionRepository;
import com.dgu.review.domain.myarchive.dto.AnswerCheckItem;
import com.dgu.review.domain.myarchive.dto.CursorPageResponse;
import com.dgu.review.domain.myarchive.dto.MyInterviewListItemResponse;
import com.dgu.review.domain.myarchive.dto.MyInterviewSummaryResponse;
import com.dgu.review.domain.myarchive.dto.MyInterviewTitleUpdateRequest;
import com.dgu.review.domain.myarchive.dto.MyInterviewTitleUpdateResponse;
import com.dgu.review.domain.myarchive.dto.MyinterviewfeedbackResponse;
import com.dgu.review.domain.myarchive.dto.RootQuestionCard;
import com.dgu.review.domain.peerfeedback.repository.PeerFeedbackRepository;
import com.dgu.review.domain.user.service.GetUserService;
import com.dgu.review.global.exception.ApiException;
import com.dgu.review.global.exception.ErrorCode;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MyInterviewService {

    private final InterviewSessionRepository interviewSessionRepository;
    private final InterviewQuestionRepository interviewQuestionRepository;
    private final GetUserService getUserService;
    private final InterviewGetUrlService interviewGetUrlService;
    private final PeerFeedbackRepository peerFeedbackRepository;

    // 면접 목록 조회 
    public CursorPageResponse<MyInterviewListItemResponse> getMyInterviews(Long cursor, int limit) {
    	Long userId = getUserService.getUserId();
        // limit+1로 더 가져와서 hasNext 판별
        int fetchSize = Math.min(Math.max(limit, 1), 50) + 1;
        var pageable = PageRequest.of(0, fetchSize);

        List<InterviewSession> rows =
                interviewSessionRepository.findSliceByUserId(userId, cursor, pageable);

        boolean hasNext = rows.size() > limit;
        if (hasNext) {
            rows = rows.subList(0, limit);
        }

        var items = rows.stream()
                .map(s -> new MyInterviewListItemResponse(s.getId(), s.getTitle()))
                .toList();

        Long nextCursor = items.isEmpty() ? null : items.get(items.size() - 1).interviewId();

        return new CursorPageResponse<>(items, nextCursor, hasNext);
    }
    
    
    // 면접 조회 - 전체 요약 
    @Transactional
    public MyInterviewSummaryResponse getMyInterviewSummary(Long interviewId) {
        Long userId = getUserService.getUserId();

        InterviewSession session = interviewSessionRepository
                .findByIdAndUserId(interviewId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.INTERVIEW_SESSION_NOT_FOUND));

        List<InterviewQuestion> all = session.getQuestions();

        List<InterviewQuestion> roots = all.stream()
                .filter(q -> q.getParentQuestion() == null)
                .sorted(Comparator.comparing(q -> q.getQuestionNumber() == null ? Integer.MAX_VALUE : q.getQuestionNumber()))
                .toList();

        final int[] orderIdx = {1};
        List<RootQuestionCard> RootQuestions = roots.stream()
                .map(q -> RootQuestionCard.builder()
                        .order(orderIdx[0]++)
                        .questionId(q.getId())
                        .build())
                .toList();

        List<AnswerCheckItem> firstThread = List.of();
        if (!roots.isEmpty()) {
            List<InterviewQuestion> chain = new ArrayList<>();
            Set<Long> seen = new HashSet<>();
            InterviewQuestion cur = roots.get(0);

            while (cur != null && (cur.getId() == null || seen.add(cur.getId()))) {
                chain.add(cur);
                InterviewQuestion next = cur.getFollowUpQuestion();
                cur = next;
            }
            

            final int[] threadIdx = {1};
            firstThread = chain.stream()
                    .map(q -> {
                        Recording rec = q.getRecording();
                        String answerText = rec != null ? rec.getSttText() : null;
                        String key = rec.getObjectKey();
                        String Url=interviewGetUrlService.createRecordingGetUrl(key);
                        return AnswerCheckItem.builder()  
                                .order(threadIdx[0]++)
                                .questionId(q.getId())
                                .question(q.getQuestion())
                                .answerText(answerText)
                                .recordUrl(Url)
                                .build();
                    })
                    .toList();
        }

        int timedOutCount = session.getTimeoutQuestionNumber();

        return MyInterviewSummaryResponse.builder()
                .title(session.getTitle())
                .timedOutCount(timedOutCount)
                .questionCards(RootQuestions)
                .firstQuestionThread(firstThread)
                .build();
    }
    // 면접 조회 - 답변 확인
    @Transactional
    public List<AnswerCheckItem> getMyInterviewAnswer(Long questionId) {
        Long userId = getUserService.getUserId();

        InterviewQuestion start = interviewQuestionRepository
                .findByIdAndUserId(questionId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.INTERVIEW_QUESTION_NOT_FOUND));

        // 루트 질문이 아닐 경우 
        InterviewQuestion root = start;
        Set<Long> guard = new HashSet<>();
        while (root.getParentQuestion() != null && (root.getId() == null || guard.add(root.getId()))) {
            root = root.getParentQuestion();
        }

        //루트부터 followUp 체인 내려가기
        List<InterviewQuestion> chain = new ArrayList<>();
        guard.clear();
        InterviewQuestion cur = root;
        while (cur != null && (cur.getId() == null || guard.add(cur.getId()))) {
            chain.add(cur);
            cur = cur.getFollowUpQuestion();
        }

        final int[] orderIdx = {1};
        return chain.stream().map(q -> {
            Recording rec = q.getRecording();
            String answerText = (rec != null) ? rec.getSttText() : null;

            String recordUrl = null;
            if (rec != null && rec.getObjectKey() != null) {
                recordUrl = interviewGetUrlService.createRecordingGetUrl(rec.getObjectKey());
            }

            return AnswerCheckItem.builder()
                    .order(orderIdx[0]++)
                    .questionId(q.getId())
                    .question(q.getQuestion())
                    .answerText(answerText)
                    .recordUrl(recordUrl)
                    .build();
        }).toList();
    }
    // 면접 조회 - 피드백 확인 
    @Transactional
    public MyinterviewfeedbackResponse getQuestionFeedback(Long questionId) {
        Long userId = getUserService.getUserId();

        InterviewQuestion q = interviewQuestionRepository
                .findByIdAndUserId(questionId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.INTERVIEW_QUESTION_NOT_FOUND));

        // recording이 없으면 null 반환  
        List<String> peerItems = Optional.ofNullable(q.getRecording())
                .map(Recording::getId)
                .map(peerFeedbackRepository::findByRecording_Id)
                .orElseGet(Collections::emptyList)
                .stream()
                .map(pf -> pf.getBody())
                .toList();

        return new MyinterviewfeedbackResponse(
                q.getAiFeedback(),
                q.getSelfFeedback(),
                peerItems
        );
    }
    
    // 면접 제목 수정 
    @Transactional
    public MyInterviewTitleUpdateResponse updateTitle(Long interviewId, MyInterviewTitleUpdateRequest req) {
    	Long userId = getUserService.getUserId();
        final String newTitle = req.title().trim();

        InterviewSession session = interviewSessionRepository
                .findByIdAndUserId(interviewId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.INTERVIEW_SESSION_NOT_FOUND));

        String oldTitle = session.getTitle() == null ? "" : session.getTitle().trim();

        if (!oldTitle.equals(newTitle)) {
        	// 다른 제목일 경우 -> 수정 
            session.changeTitle(newTitle);
        }

        return new MyInterviewTitleUpdateResponse(session.getId(), session.getTitle());
    }
    
    
}