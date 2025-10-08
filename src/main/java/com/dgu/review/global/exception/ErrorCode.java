package com.dgu.review.global.exception;

import org.springframework.http.HttpStatus;


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
    
	// 질문 없음
	QUESTION_NOT_FOUND("QUESTION_NOT_FOUND",
			HttpStatus.NOT_FOUND, "해당 질문이 존재하지 않습니다."),
	// 질문의 소유자가 아님 
	FORBIDDEN_RESOURCE("FORBIDDEN_RESOURCE", 
			HttpStatus.FORBIDDEN, "해당 질문에 접근 권한이 없습니다."),
	// 파일 이름에서 확장자 인식 불가 
	RESUME_EXTENSION_MISSING("RESUME_EXTENSION_MISSING",
		    HttpStatus.BAD_REQUEST, "파일 확장자를 인식할 수 없습니다. pdf 또는 docx 파일을 올려주세요."),
	// 지원하지 않는 파일 형식 
	RESUME_UNSUPPORTED_MEDIA_TYPE( "RESUME_UNSUPPORTED_MEDIA_TYPE",
			HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 파일 형식입니다."),
	// 저장소에서 자소서 찾을 수 없음 
	STORAGE_RESUME_NOT_FOUND( "STORAGE_RESUME_NOT_FOUND",
			HttpStatus.NOT_FOUND, "저장소에서 해당 자소서를 찾을 수 없습니다. "),
	// 저장소에서 녹음 찾을 수 없음
	STORAGE_RECORDING_NOT_FOUND( "STORAGE_RECORDING_NOT_FOUND",
			HttpStatus.NOT_FOUND, "저장소에서 해당 녹음 찾을 수 없습니다."),
	FORBIDDEN_STORAGE("FORBIDDEN_STORAGE", 
			HttpStatus.FORBIDDEN, "해당 저장소에 접근 권한이 없습니다."),
	// 저장소에서 자소서 찾을 수 없음
	STORAGE_UNAVAILABLE( "STORAGE_UNAVAILABLE",
			HttpStatus.NOT_FOUND, "저장소에서 장애가 발생했습니다. "),
	//자소서 추출 실패 
	RESUME_TEXT_EXTRACTION_FAILED( "RESUME_TEXT_EXTRACTION_FAILED",
			HttpStatus.UNPROCESSABLE_ENTITY, "저장소에서 장애가 발생했습니다. "),
	//자소서 비밀번호 요구 
	RESUME_PASSWORD_REQUIRED( "RESUME_PASSWORD_REQUIRED",
			HttpStatus.UNPROCESSABLE_ENTITY, "자소서가 비밀번호를 필요로 합니다. 비밀번호 제거 후 파일을 올려주세요."),
	//자소서에 내용이 없음 
	EMPTY_RESUME( "EMPTY_RESUME",
				HttpStatus.UNPROCESSABLE_ENTITY, "자소서에 내용이 없습니다. ");
	
	
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
