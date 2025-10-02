package com.dgu.review.global.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

public enum ErrorCode {

	BAD_REQUEST("BAD_REQUEST",HttpStatus.BAD_REQUEST, "잘못된 요청입니다. "),
    // 인터뷰 질문 없음
    INTERVIEW_QUESTION_NOT_FOUND("INTERVIEW_QUESTION_NOT_FOUND",
            HttpStatus.BAD_REQUEST, "인터뷰 질문이 없습니다."),

    // 세션 불일치
    INTERVIEW_SESSION_MISMATCH("INTERVIEW_SESSION_MISMATCH",
            HttpStatus.BAD_REQUEST, "세션 불일치 오류가 발생했습니다."),

    // 녹음 없음
    RECORDING_NOT_FOUND("RECORDING_NOT_FOUND",
            HttpStatus.BAD_REQUEST, "녹음을 찾을 수 없습니다."),

    // 세션에 녹음 없음
    SESSION_RECORDINGS_NOT_FOUND("SESSION_RECORDINGS_NOT_FOUND",
            HttpStatus.BAD_REQUEST, "해당 세션에 녹음이 존재하지 않습니다."),

	FOLLOWUP_QUESTION_NOT_FOUND("FOLLOWUP_QUESTION_NOT_FOUND",
								 HttpStatus.BAD_REQUEST, "해당 질문에 꼬리질문이 존재하지 않습니다."),

	ALREADY_IN_QUEUE_OR_DONE("ALREADY_IN_QUEUE_OR_DONE",
			HttpStatus.BAD_REQUEST, "해당 녹음이 이미 큐에 존재하거나 처리가 완료되었습니다."),

	RECORDING_ALREADY_PROCESSED("RECORDING_ALREADY_PROCESSED",
			HttpStatus.BAD_REQUEST, "해당 id에 해당하는 녹음이 이미 처리되었습니다."),
	;
	private final String code;
	private final HttpStatus status;
	private final String defaultMessage;

	ErrorCode(String code, HttpStatus status, String defaultMessage) {
		this.code = code;
		this.status = status;
		this.defaultMessage = defaultMessage;

	}

	public String code() {
		return code;
	}

	public HttpStatus status() {
		return status;
	}

	public String defaultMessage() {
		return defaultMessage;
	}

}
