package com.dgu.review.domain.interview.dto;

import java.util.Map;

public record PresignRecordingUploadResponse(
        String uploadUrl,
        String key,
        Map<String, String> requiredHeaders // 클라이언트가 그대로 넣어야 하는 헤더
) {}