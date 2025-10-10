package com.dgu.review.domain.interview.dto;


import com.dgu.review.domain.interview.entity.InterviewMode;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InterviewCreateRequest(
     @NotNull InterviewMode mode,          // 면접 모드 
     @NotNull String  jobRole,         // 희망 직군 
     @Size(max = 512) String resumeId 

) {}




