package com.dgu.review.domain.interview.service;

import java.net.URL;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@Service
@RequiredArgsConstructor
public class InterviewGetUrlService {

    private final S3Presigner presigner;
    @Value("${aws.s3.bucket}")
    private String bucket;

    // 자소서 GET presigned URL 
    public String createResumeGetUrl(Long userId, String resumeId, String ext) {
        String key = "resume/%d/%d.%s".formatted(userId,resumeId,ext);

        // 10분 유효
        long expiryMinutes = 10;

        GetObjectRequest get = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        GetObjectPresignRequest presign = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(expiryMinutes))
                .getObjectRequest(get)
                .build();

        PresignedGetObjectRequest signed = presigner.presignGetObject(presign);
        URL url = signed.url();

        return url.toString();
    }

 // 녹음 GET presigned URL
    public String createRecordingGetUrl(Long userId, Long questionId, String ext) {
        String key = "recording/%d/%d.%s".formatted(userId,questionId, ext);

        long expiryMinutes = 10;

        GetObjectRequest get = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        GetObjectPresignRequest presign = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(expiryMinutes))
                .getObjectRequest(get)
                .build();

        PresignedGetObjectRequest signed = presigner.presignGetObject(presign);
        URL url = signed.url();

        return url.toString();

    }
}
