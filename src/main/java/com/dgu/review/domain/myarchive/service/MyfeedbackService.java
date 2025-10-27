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
import com.dgu.review.domain.myarchive.dto.MyPeerFeedbackListItemResponse;
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
			return new MyPeerFeedbackListItemResponse(pf.getId(), jobRole);
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

        return new MyPeerFeedbackResponse(
                jobRole,
                formattedDate,
                question.getQuestion(),        
                feedback.getBody(),
                feedback.getFollowUpQuestion()  
        );
    }
    
    

    
}
