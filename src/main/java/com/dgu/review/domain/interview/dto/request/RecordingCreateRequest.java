package com.dgu.review.domain.interview.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

//사용자가 “새로운 녹음 파일을 업로드/등록할 때” 요청 바디로 전송.
@Getter @Setter
public class RecordingCreateRequest //client -> server 요청 데이터 DTO
{
    @NotNull private Long interviewQuestionId; //어떤 인터뷰 질문에 대한 답변인지.
    @NotBlank private String objectKey; //S3에 저장된 오디오 파일의 경로 키
}
