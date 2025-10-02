package com.dgu.review.domain.interview.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PresignRecordUploadRequest(
     @NotNull Long questionId,
     //나중에 녹음 파일 고정시 제거 
     @NotBlank String contentType      // 예: audio/webm, audio/mpeg
) {}
