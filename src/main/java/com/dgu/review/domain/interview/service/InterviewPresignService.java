
package com.dgu.review.domain.interview.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.dgu.review.domain.interview.dto.RecordingUploadUrlRequest;
import com.dgu.review.domain.interview.dto.RecordingUploadUrlResponse;
import com.dgu.review.domain.interview.dto.ResumeUploadUrlResponse;
import com.dgu.review.domain.interview.repository.InterviewQuestionRepository;
import com.dgu.review.global.exception.ApiException;
import com.dgu.review.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class InterviewPresignService {

 private final S3Presigner presigner;
 @Value("${aws.s3.bucket}")
 private String bucket;
 private final InterviewQuestionRepository interviewQuestionRepository;
 private final StringRedisTemplate redisTemplate;

 public RecordingUploadUrlResponse createRecordingPutUrl(RecordingUploadUrlRequest req, Long userId) {
     Long questionId=req.questionId();
     
	 // questionid가 실제 db에 있는지 검증 
	 if(!interviewQuestionRepository.existsById(questionId)) {
		 throw new ApiException(ErrorCode.QUESTION_NOT_FOUND);
	 };
	 // questionid가 user의 소유가 맞는지 검증 
	 if (!interviewQuestionRepository.existsByIdAndInterviewSessionUserId(questionId, userId)) {
         throw new ApiException(ErrorCode.FORBIDDEN_RESOURCE);
     }
	 
	 // url 유효시간 10분 
	 long expiryMinutes=10;
	 
	 // contentType을 확장자로 
     String ext = mapExt(req.contentType());
     String key = "recording/%d/%d.%s".formatted(userId, questionId, ext);

     PutObjectRequest put = PutObjectRequest.builder()
             .bucket(bucket)
             .key(key)
             .contentType(req.contentType())
             .build();

     PutObjectPresignRequest presign = PutObjectPresignRequest.builder()
             .signatureDuration(Duration.ofMinutes(expiryMinutes))
             .putObjectRequest(put)
             .build();

     PresignedPutObjectRequest signed = presigner.presignPutObject(presign);
     URL url = signed.url();

     // url로 업로드 시 반드시 동일하게 보내야 검증 통과하는 헤더 (특히 Content-Type)
     Map<String, String> headers = new HashMap<>();
     headers.put("Content-Type", req.contentType());
     
     //key 값 redis에 저장 
     String redisKey="presign:recording:"+ questionId;
     redisTemplate.opsForValue().set(redisKey, key, Duration.ofMinutes(10));

     return new RecordingUploadUrlResponse(
             url.toString(),
             key,
             headers
     );
 }

 public ResumeUploadUrlResponse createResumePutUrl(Long userId, String fileName) {
	 // 랜덤으로 resumeId 생성 
	 String resumeId = UUID.randomUUID().toString();  
	 
	 // url 유효시간 10분 
	 long expiryMinutes=10;
	 
	 // fileName에서 contentType을 가져옴 
	 String ext = extractExt(fileName);      
	    if (ext == null) {
	        throw new ApiException(ErrorCode.RESUME_EXTENSION_MISSING); 
	    }
	    String contentType = mapResumeContentType(ext); 
	    if (contentType == null) {
	        throw new ApiException(ErrorCode.RESUME_UNSUPPORTED_MEDIA_TYPE);
	    }
     String key = "resume/%d/%s.%s".formatted(userId,resumeId,ext);

     PutObjectRequest put = PutObjectRequest.builder()
             .bucket(bucket)
             .key(key)
             .contentType(contentType)
             .build();

     PutObjectPresignRequest presign = PutObjectPresignRequest.builder()
             .signatureDuration(Duration.ofMinutes(expiryMinutes))
             .putObjectRequest(put)
             .build();

     PresignedPutObjectRequest signed = presigner.presignPutObject(presign);
     URL url = signed.url();

     // url로 업로드 시 반드시 동일하게 보내야 검증 통과하는 헤더 (특히 Content-Type)
     Map<String, String> headers = new HashMap<>();
     headers.put("Content-Type", contentType);

     //key 값 redis에 저장 
     String redisKey="presign:resume:"+ resumeId;
     redisTemplate.opsForValue().set(redisKey, key, Duration.ofMinutes(10));
     
     return new ResumeUploadUrlResponse(
             url.toString(),
             key,
             headers,
             resumeId
     );
 }



 //나중에 음성 파일 확장명이 고정되면 제거 
 //나중에 다른 확장자가 들어오면 에러 뜨도록 
 private String mapExt(String contentType) {
     return switch (contentType) {
         case "audio/webm" -> "webm";
         case "audio/mpeg" -> "mp3";
         case "audio/wav", "audio/x-wav" -> "wav";
         case "audio/mp4" -> "m4a";
         default -> "bin"; // 알 수 없으면 이진
     };
 }
 
 
 private String extractExt(String fileName) {
	    String name = fileName.replace("\\", "/");
	    int slash = name.lastIndexOf('/');
	    if (slash >= 0) name = name.substring(slash + 1);

	    int dot = name.lastIndexOf('.');
	    if (dot < 0 || dot == name.length() - 1) return null;
	    return name.substring(dot + 1).toLowerCase();
	}

private String mapResumeContentType(String ext) {
	    return switch (ext) {
	        case "pdf"  -> "application/pdf";
	        case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
	        default     -> null; // 허용 아님
	    };
	}

    public String getRecordingObjectKey(Long questionId) {
        String redisKey = "presign:recording:" + questionId;
        String key = redisTemplate.opsForValue().get(redisKey);
        if (key == null) {
            throw new ApiException(ErrorCode.REDIS_KEY_NOT_FOUND);
        }
        return key;
    }
}


