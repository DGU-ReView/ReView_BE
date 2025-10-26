package com.dgu.review.domain.myarchive.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;


import com.dgu.review.domain.myarchive.dto.CursorPageResponse;
import com.dgu.review.domain.myarchive.dto.MyFeedbackListItemResponse;
import com.dgu.review.domain.peerfeedback.entity.PeerFeedback;
import com.dgu.review.domain.peerfeedback.repository.PeerFeedbackRepository;
import com.dgu.review.domain.user.service.GetUserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MyfeedbackService {
	private final PeerFeedbackRepository peerFeedbackRepository;
	private final GetUserService getUserService;
	private final String FEEDBACK_TITLE_SUFFIX = " 직무의 사용자에게 남긴 피드백";

	// 내가 한 피어 피드백 목록 조회
	public CursorPageResponse<MyFeedbackListItemResponse> getMyFeedbacks(Long cursor, int limit) {

		Long userId = getUserService.getUserId();

		// limit+1로 더 가져와서 hasNext 판별
		int fetchSize = Math.min(Math.max(limit, 1), 50) + 1;
		var pageable = PageRequest.of(0, fetchSize);

		List<PeerFeedback> rows = peerFeedbackRepository.findSliceByUserId(userId, cursor, pageable);

		boolean hasNext = rows.size() > limit;
		if (hasNext) {
			rows = rows.subList(0, limit);
		}

		List<MyFeedbackListItemResponse> items = rows.stream().map(pf -> {
			String jobRole = pf.getRecording().getInterviewQuestion().getInterviewSession().getJobRole();
			String title = jobRole + FEEDBACK_TITLE_SUFFIX;
			return new MyFeedbackListItemResponse(pf.getId(), title);
		}).toList();

		Long nextCursor = items.isEmpty() ? null : items.get(items.size() - 1).peerFeedbackId();
		return new CursorPageResponse<>(items, nextCursor, hasNext);
	}
}
