package com.dgu.review.domain.interview.service;

import com.dgu.review.domain.interview.dto.InterviewCreateRequest;
import com.dgu.review.domain.interview.entity.InterviewSession;
import com.dgu.review.domain.interview.repository.InterviewSessionRepository;
import com.dgu.review.domain.user.entity.User;
import com.dgu.review.domain.user.repository.UserRepository;
import com.dgu.review.domain.user.service.GetUserService;
import com.dgu.review.global.exception.ApiException;
import com.dgu.review.global.exception.ErrorCode;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;



@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewPreparationService {
	
    private final InterviewObjectReadService objectReadService;
    private final AutoDetectParser parser = new AutoDetectParser();
    private final RedisTemplate<String, String> redisTemplate;
    private final InterviewSessionRepository interviewSessionRepository;	
    private final UserRepository userRepository;
    private static final String PRESIGN_RESUME_KEY_PREFIX = "presign:resume:";
    private final GetUserService getUserService;
    @Transactional
    public String extractText(InterviewCreateRequest req) {
    	String resumeId=req.resumeId();	
    	String redisKey= PRESIGN_RESUME_KEY_PREFIX+resumeId;
    	String resumeObjectKey=redisTemplate.opsForValue().get(redisKey);
    	// resumeObjectKey가 없을 경우 
    	if (resumeObjectKey == null || resumeObjectKey.isBlank()) {
		    throw new ApiException(ErrorCode.STORAGE_RESUME_NOT_FOUND); 
		}
    	
    	saveInterviewSession(req,resumeObjectKey);
    	
        // S3 열기 
        try (var in = objectReadService.openResume(resumeObjectKey)) {
        	//자소서 변환 
            BodyContentHandler handler = new BodyContentHandler(-1); // 길이 제한 없음 
            Metadata metadata = new Metadata();
            ParseContext context = new ParseContext();
            parser.parse(in, handler, metadata, context);
            String extraction = handler.toString();
            
            //자소서 필터링 
            String resumeText = resumeFilter(resumeId, extraction);
            return resumeText;
            
        } catch (ApiException e) {
            // openResume에서 이미 발생한 에러 
            throw e;
        } catch (org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException e) {
            // PDF 암호 때문에 열지 못함
            throw new ApiException(ErrorCode.RESUME_PASSWORD_REQUIRED); 
        }catch (Exception e) {
            log.error("Resume text extraction failed. key={}, cause={}", resumeObjectKey, e.toString());
            throw new ApiException(ErrorCode.RESUME_TEXT_EXTRACTION_FAILED);
        }
    }
    
    // db에 인터뷰 섹션 저장 
    private void saveInterviewSession(InterviewCreateRequest req,String resumeObjectKey) {
    	//목 userId
    	Long userId = getUserService.getUserId();
    	User user = userRepository.findById(userId)
    				    .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
    	InterviewSession session = InterviewSession.builder() 
    			        .resumeObjectKey(resumeObjectKey)
    			        .mode(req.mode())
    			        .jobRole(req.jobRole())
    			        .user(user)
    			        .build();

    	interviewSessionRepository.save(session);
    }
    
    // 자소서 필터링 
    private String resumeFilter(String resumeId, String text) {
    	
 		// 텍스트에 아무것도 없을 경우 
 		if (text == null || text.isBlank()) {
             log.error("Empty text in resume id={}", resumeId);
             throw new ApiException(ErrorCode.EMPTY_RESUME);
         }
 		//내용 필터링 추가
 		
 		return text;
 	}
}
