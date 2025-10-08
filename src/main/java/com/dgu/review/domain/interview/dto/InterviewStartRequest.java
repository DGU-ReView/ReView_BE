package com.dgu.review.domain.interview.dto;


import com.dgu.review.domain.interview.entity.InterviewMode;
import com.dgu.review.domain.interview.entity.JobDomain;
import com.dgu.review.domain.interview.entity.JobRole;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InterviewStartRequest(
     @NotNull InterviewMode mode,          // 면접 모드 
     @NotNull JobDomain desiredDomain,     // 희망 분야 
     @NotNull JobRole desiredRole,         // 희망 직군 
     @Size(max = 512) String resumeId 

) {}




