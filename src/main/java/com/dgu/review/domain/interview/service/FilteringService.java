package com.dgu.review.domain.interview.service;

import com.dgu.review.global.exception.ApiException;
import com.dgu.review.global.exception.ErrorCode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FilteringService {
	public String resumeFilter(String resumeId,String text) {
		// 텍스트에 아무것도 없을 경우 
		if (text == null || text.isBlank()) {
            log.error("Empty text in resume id={}", resumeId);
            throw new ApiException(ErrorCode.EMPTY_RESUME);
        }
		//내용 필터링 추가
		
		return text;
	}
}
