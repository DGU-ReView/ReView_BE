package com.dgu.review.domain.myarchive.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.dgu.review.domain.interview.entity.InterviewQuestion;
import com.dgu.review.domain.interview.entity.InterviewSession;
import com.dgu.review.domain.interview.entity.Recording;
import com.dgu.review.domain.myarchive.dto.CursorPageResponse;
import com.dgu.review.domain.myarchive.dto.MyPeerFeedbackCardResponse;
import com.dgu.review.domain.myarchive.dto.MyPeerFeedbackListItemResponse;
import com.dgu.review.domain.myarchive.dto.MyPeerFeedbackQuestionCardResponse;
import com.dgu.review.domain.myarchive.dto.MyPeerFeedbackResponse;
import com.dgu.review.domain.peerfeedback.entity.PeerFeedback;
import com.dgu.review.domain.peerfeedback.repository.PeerFeedbackRepository;
import com.dgu.review.domain.user.service.GetUserService;
import com.dgu.review.global.exception.ApiException;
import com.dgu.review.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MyfeedbackService {
	private final PeerFeedbackRepository peerFeedbackRepository;
	private final GetUserService getUserService;
	private final String FEEDBACK_TITLE_SUFFIX = " 직무의 사용자에게 남긴 피드백";
	private final String My_FEEDBACK_TITLE=" 직무의 사용자에게 아래와 같은 피드백을 남겼어요";
	private final String My_QUESTION_TITLE=" 직무의 사용자에게 아래와 같은 질문을 남겼어요";

	// 내가 한 피어 피드백 목록 조회
	public CursorPageResponse<MyPeerFeedbackListItemResponse> getMyFeedbacks(Long cursor, int limit) {

		Long userId = getUserService.getUserId();

		// limit+1로 더 가져와서 hasNext 판별
		int fetchSize = Math.min(Math.max(limit, 1), 50) + 1;
		var pageable = PageRequest.of(0, fetchSize);

		List<PeerFeedback> rows = peerFeedbackRepository.findSliceByUserId(userId, cursor, pageable);

		boolean hasNext = rows.size() > limit;
		if (hasNext) {
			rows = rows.subList(0, limit);
		}

		List<MyPeerFeedbackListItemResponse> items = rows.stream().map(pf -> {
			String jobRole = pf.getRecording().getInterviewQuestion().getInterviewSession().getJobRole();
			String title = jobRole + FEEDBACK_TITLE_SUFFIX;
			return new MyPeerFeedbackListItemResponse(pf.getId(), title);
		}).toList();

		Long nextCursor = items.isEmpty() ? null : items.get(items.size() - 1).peerFeedbackId();
		return new CursorPageResponse<>(items, nextCursor, hasNext);
	}
	
	//내가 한 피어 피드백 상세 조회 
    public MyPeerFeedbackResponse getFeedbackDetail(Long feedbackId) {

        Long currentUserId = getUserService.getUserId();
        PeerFeedback feedback = peerFeedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new ApiException(ErrorCode.PEER_FEEDBACK_NOT_FOUND));


        if (!feedback.getUser().getId().equals(currentUserId)) {
            throw new ApiException(ErrorCode.FORBIDDEN_RESOURCE);
        }

        Recording recording = feedback.getRecording();
        InterviewQuestion question = recording.getInterviewQuestion();
        InterviewSession session = question.getInterviewSession();
        String jobRole = session.getJobRole();
        LocalDateTime createdAt = feedback.getCreatedAt();
        String formattedDate = createdAt.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일"));


        String FeedbackTitle = "["+formattedDate + "] "+ jobRole + My_FEEDBACK_TITLE;
        String QuestionTitle = "["+formattedDate + "] "+ jobRole + My_QUESTION_TITLE;

        MyPeerFeedbackCardResponse feedbackCard = new MyPeerFeedbackCardResponse(
        		FeedbackTitle,
        		question.getQuestion(),
                feedback.getBody()
        );

        MyPeerFeedbackQuestionCardResponse questionCard = new MyPeerFeedbackQuestionCardResponse(
        		QuestionTitle,
                feedback.getFollowUpQuestion()
        );

        return new MyPeerFeedbackResponse(feedbackCard, questionCard);
    }
    
    

    
}
