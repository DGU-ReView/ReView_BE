package com.dgu.review.domain.interview.service;

import java.net.URL;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.dgu.review.global.exception.ApiException;
import com.dgu.review.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewObjectReadService {

    private final S3Client s3;
    private final S3Presigner presigner;

    @Value("${aws.s3.bucket}")
    private String bucket;

    // 자소서 InputStream 반환 (호출자가 닫아야 함)
    public ResponseInputStream<GetObjectResponse> openResume(Long userId, String resumeId, String ext) {
        String key = "resume/%d/%s.%s".formatted(userId, resumeId, ext);
        return getObjectOrThrow(key, ErrorCode.STORAGE_RESUME_NOT_FOUND);
    }

    // 녹음 InputStream 반환
    public ResponseInputStream<GetObjectResponse> openRecording(Long userId, Long questionId, String ext) {
        String key = "recording/%d/%d.%s".formatted(userId, questionId, ext);
        return getObjectOrThrow(key, ErrorCode.STORAGE_RECORDING_NOT_FOUND);
    }
    
    // 녹음 get url 반환 
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
    
    
    
    //예외 처리 
    private ResponseInputStream<GetObjectResponse> getObjectOrThrow(String key, ErrorCode notFoundCode) {
        try {
            return s3.getObject(b -> b.bucket(bucket).key(key));
        } catch (NoSuchKeyException e) {
            throw new ApiException(notFoundCode);
        } catch (S3Exception e) {
            int sc = e.statusCode();
            if (sc == 404 ) {
            	log.info("key :{}인 리소스를 s3에서 찾을 수 없습니다. ",key);
            	
            	throw new ApiException(notFoundCode);
            }
            if (sc == 403 ) throw new ApiException(ErrorCode.FORBIDDEN_STORAGE);

            // 기타 4xx/5xx는 저장소 장애로 통합
            throw new ApiException(ErrorCode.STORAGE_UNAVAILABLE);
        } 
    }
    
    
  
}

