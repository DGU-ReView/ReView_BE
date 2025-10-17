package com.dgu.review.domain.myarchive.service;

import java.util.List;


import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.dgu.review.domain.interview.entity.InterviewSession;
import com.dgu.review.domain.interview.repository.InterviewSessionRepository;
import com.dgu.review.domain.myarchive.dto.CursorPageResponse;
import com.dgu.review.domain.myarchive.dto.MyInterviewListItemResponse;
import com.dgu.review.domain.myarchive.dto.MyInterviewTitleUpdateRequest;
import com.dgu.review.domain.myarchive.dto.MyInterviewTitleUpdateResponse;
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
    private Long userId = 123L;

    // 면접 목록 조회 
    public CursorPageResponse<MyInterviewListItemResponse> getMyInterviews(Long cursor, int limit) {

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
    
    // 면접 제목 수정 
    @Transactional
    public MyInterviewTitleUpdateResponse updateTitle(Long interviewId, MyInterviewTitleUpdateRequest req) {

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