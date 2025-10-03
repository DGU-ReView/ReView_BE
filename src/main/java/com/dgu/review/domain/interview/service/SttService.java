package com.dgu.review.domain.interview.service;

import com.dgu.review.domain.interview.dto.request.RecordingCreateRequest;
import com.dgu.review.domain.interview.dto.response.*;
import com.dgu.review.domain.interview.entity.InterviewQuestion;
import com.dgu.review.domain.interview.entity.Recording;
import com.dgu.review.domain.interview.entity.RecordingStatus;
import com.dgu.review.domain.interview.repository.InterviewQuestionRepository;
import com.dgu.review.domain.interview.repository.RecordingRepository;
import com.dgu.review.global.exception.ApiException;
import com.dgu.review.global.exception.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import lombok.extern.slf4j.Slf4j;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class SttService {

    private final RecordingRepository recordingRepo;
    private final RecordingStatusService statusService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final SttFeedbackService sttFeedbackService;
    private final InterviewQuestionRepository interviewQuestionRepository;

//    @Value("${stt.worker.base-url}")
//    private String workerBaseUrl;
//
//    @Value("${app.s3.bucket}")
//    private String s3Bucket;

    @Transactional
    public SttStatusResponse startSttProcessing(Long recordingId, Long sessionId, Long questionId, RecordingCreateRequest request) {
//        Recording r = recordingRepo.findById(recordingId)
//                .orElseThrow(() -> {
//                    log.warn("녹음을 찾을 수 없습니다. recordingId={}", recordingId); // 내부 로그
//                    return new ApiException(ErrorCode.RECORDING_NOT_FOUND);
//                });

        RecordingStatus current = statusService.getStatus(recordingId);
        if (current == RecordingStatus.TRANSCRIBING || current == RecordingStatus.COMPLETED) {
            return new SttStatusResponse(current.name());
        }

        //statusService.setStatus(r.getId(), RecordingStatus.TRANSCRIBING);
        //sttAsyncWorker(r); // 비동기

        return new SttStatusResponse(RecordingStatus.TRANSCRIBING.name());
    }



    /**
     * Whisper large-v3를 실행하는 비동기 워커
     */
    @Transactional
    @Async
    public void sttAsyncWorker(Recording recording) {
        try {
            long started = System.currentTimeMillis();
            log.info("[sttWorker:start] recordingId={}, thread={}, audioKey={}",
                    recording.getId(), Thread.currentThread().getName(), recording.getObjectKey());

            statusService.setStatus(recording.getId(), RecordingStatus.TRANSCRIBING, null);
            log.info("[sttWorker:status] recordingId={} -> TRANSCRIBING", recording.getId());
            log.info("[status:readback] recordingId={}, now={}", recording.getId(), statusService.getStatus(recording.getId()));

            String projectRoot = System.getProperty("user.dir");
            String audioPath = projectRoot + File.separator + "audio_files" + File.separator + recording.getObjectKey();
            String scriptPath = projectRoot + File.separator + "stt_worker" + File.separator + "transcribe.py";

            log.info("[sttWorker:cmd] recordingId={}, projectRoot={}, scriptPath={}, audioPath={}",
                    recording.getId(), projectRoot, scriptPath, audioPath);

            ProcessBuilder pb = new ProcessBuilder("python", scriptPath, audioPath);
            pb.directory(new File(projectRoot));
            pb.redirectErrorStream(true);


            Process process = pb.start();
            log.info("[sttWorker:proc] recordingId={} processStarted (thread={})",
                    recording.getId(), Thread.currentThread().getName());
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
            int exitCode = process.waitFor();
            log.info("[sttWorker:exit] recordingId={}, exitCode={}, elapsedMs={}", recording.getId(), exitCode, System.currentTimeMillis()-started);

            if (exitCode == 0) {
                recordingRepo.updateSttTextById(recording.getId(), output.toString());
                statusService.setStatus(recording.getId(), RecordingStatus.COMPLETED, null);

                String followUpQuestionText = sttFeedbackService.generateAiFeedback(recording.getId(), recording.getInterviewQuestion().getId());

                statusService.setStatus(recording.getId(), RecordingStatus.FOLLOWUP_GENERATED, Duration.ofDays(1));

                InterviewQuestion followUpQuestion = interviewQuestionRepository.save(InterviewQuestion.builder()
                        .question(followUpQuestionText)
                        .interviewSession(recording.getInterviewQuestion().getInterviewSession())
                        .parentQuestion(recording.getInterviewQuestion())
                        .build()
                );

                recording.getInterviewQuestion().attachFollowUp(followUpQuestion);

            } else {
                statusService.setStatus(recording.getId(), RecordingStatus.FAILED, Duration.ofMinutes(30));
            }
        } catch (Exception e) {
            statusService.setStatus(recording.getId(), RecordingStatus.UPLOADED, null);
            log.error("STT 워커 실행 실패 recordingId={}", recording.getId(), e); // 내부 로그
            throw new RuntimeException("STT 워커 실행 실패", e);
        }
    }


    @Transactional
    public void complete(Long recordingId, String transcript) {
        // 워커 완료 콜백에서 최종 텍스트 반영
        int updated = recordingRepo.updateSttTextById(recordingId, transcript == null ? "" : transcript);
        if (updated == 0) {
            log.warn("완료 처리 대상 녹음을 찾을 수 없습니다. recordingId={}", recordingId); // 내부 로그
            throw new ApiException(ErrorCode.RECORDING_NOT_FOUND);
        }

    }

    @Transactional
    public SttJobDetailResponse getDetail(Long recordingId) {
        Recording r = recordingRepo.findById(recordingId)
                .orElseThrow(() -> {
                    log.warn("녹음을 찾을 수 없습니다. recordingId={}", recordingId); // 내부 로그
                    return new ApiException(ErrorCode.RECORDING_NOT_FOUND);
                });
        RecordingStatus status = statusService.getStatus(recordingId);
        String text = status == RecordingStatus.COMPLETED ? r.getSttText() : null;

        return new SttJobDetailResponse(status.name(), text, null);
    }

    @Transactional
    public RecordingResultsResponse getRecordingResults(Long recordingId) {
        var recording = recordingRepo.findById(recordingId)
                .orElseThrow(() -> new ApiException(ErrorCode.RECORDING_NOT_FOUND));

        var status = statusService.getStatus(recordingId);
        var text = (status == RecordingStatus.COMPLETED || status == RecordingStatus.FOLLOWUP_GENERATED) ? recording.getSttText() : null;
        var followUpQuestion = (status == RecordingStatus.FOLLOWUP_GENERATED) ? recording.getInterviewQuestion().getFollowUpQuestion().getQuestion() : null;
        return new RecordingResultsResponse(recordingId, status, text, followUpQuestion);
    }

}