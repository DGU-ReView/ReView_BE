package com.dgu.review.domain.interview.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;


@Service
@RequiredArgsConstructor
public class InterviewObjectReadService {

    private final S3Client s3;

    @Value("${aws.s3.bucket}")
    private String bucket;

    // 자소서 InputStream 반환 (호출자가 닫아야 함)
    public ResponseInputStream<GetObjectResponse> openResume(Long userId, String resumeId, String ext) {
        String key = "resume/%d/%s.%s".formatted(userId, resumeId, ext);
        return s3.getObject(b -> b.bucket(bucket).key(key));
    }

    // 녹음 InputStream 반환
    public ResponseInputStream<GetObjectResponse> openRecording(Long userId, Long questionId, String ext) {
        String key = "recording/%d/%d.%s".formatted(userId, questionId, ext);
        return s3.getObject(b -> b.bucket(bucket).key(key));
    }

  
}

