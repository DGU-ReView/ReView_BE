package com.dgu.review.global.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

public enum ErrorCode {

	BAD_REQUEST("BAD_REQUEST",HttpStatus.BAD_REQUEST, "잘못된 요청입니다. ");

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
