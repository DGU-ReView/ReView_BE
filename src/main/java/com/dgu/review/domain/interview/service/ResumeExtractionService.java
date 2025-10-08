package com.dgu.review.domain.interview.service;

import com.dgu.review.global.exception.ApiException;
import com.dgu.review.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;



@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeExtractionService {

    private final InterviewObjectReadService objectReadService;
    private final FilteringService filteringService;
    private final AutoDetectParser parser = new AutoDetectParser();

    public String extractText(String resumeId, String resumeObjectKey) {
        // S3 열기 
        try (var in = objectReadService.openResume(resumeObjectKey)) {
        	//자소서 변환 
            BodyContentHandler handler = new BodyContentHandler(-1); // 길이 제한 없음 
            Metadata metadata = new Metadata();
            ParseContext context = new ParseContext();
            parser.parse(in, handler, metadata, context);
            String extraction = handler.toString();
            
            //자소서 필터링 
            String resumeText = filteringService.resumeFilter(resumeId, extraction);
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
}
