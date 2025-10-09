package com.dgu.review.domain.interview.service;

import com.dgu.review.domain.interview.entity.InterviewQuestion;
import com.dgu.review.domain.interview.entity.Recording;
import com.dgu.review.domain.interview.entity.RecordingStatus;
import com.dgu.review.domain.interview.repository.InterviewQuestionRepository;
import com.dgu.review.domain.interview.repository.RecordingRepository;
import com.dgu.review.global.exception.ApiException;
import com.dgu.review.global.exception.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;


@Slf4j
@Service
@RequiredArgsConstructor
public class RecordingTranscriber {

    private final RecordingRepository recordingRepo;
    private final RecordingStatusService statusService;
//    private final RestTemplate restTemplate = new RestTemplate();
    private final SttFeedbackService sttFeedbackService;
    private final InterviewQuestionRepository interviewQuestionRepository;
    private final InterviewObjectReadService interviewObjectReadService;

//    @Value("${stt.worker.base-url}")
//    private String workerBaseUrl;
//
//    @Value("${aws.s3.bucket}")
//    private String s3Bucket;

    /**
     * Whisper large-v3를 실행하는 비동기 워커
     */
    @Transactional
    @Async
    public void sttAsyncWorker(Long recordingId) {
        try {

            Recording recording = recordingRepo.findById(recordingId)
                    .orElseThrow(() -> new ApiException(ErrorCode.RECORDING_NOT_FOUND));

            long started = System.currentTimeMillis();
            log.info("[sttWorker:start] recordingId={}, thread={}, audioKey={}",
                    recordingId, Thread.currentThread().getName(), recording.getObjectKey());

            statusService.setStatus(recordingId, RecordingStatus.TRANSCRIBING, null);
            log.info("[status:update] recordingId={} -> TRANSCRIBING (readback={})",
                    recordingId, statusService.getStatus(recordingId));

            String audioPresignedUrl = interviewObjectReadService.createRecordingGetUrl(recording.getObjectKey());

            String projectRoot = System.getProperty("user.dir");
            String scriptPath = projectRoot + File.separator + "stt_worker" + File.separator + "transcribe.py";

            log.info("[sttWorker:cmd] recordingId={}, projectRoot={}, scriptPath={}",
                    recordingId, projectRoot, scriptPath);

            ProcessBuilder pb = new ProcessBuilder("python", scriptPath, audioPresignedUrl);
            pb.directory(new File(projectRoot));
            pb.redirectErrorStream(true);


            Process process = pb.start();
            log.info("[sttWorker:proc] recordingId={} processStarted (thread={})",
                    recordingId, Thread.currentThread().getName());
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
            int exitCode = process.waitFor();
            log.info("[sttWorker:exit] recordingId={}, exitCode={}, elapsedMs={}", recordingId, exitCode, System.currentTimeMillis()-started);

            if (exitCode == 0) {
                recording.attachSttText(output.toString());
                statusService.setStatus(recordingId, RecordingStatus.COMPLETED, null);

                String followUpQuestionText = sttFeedbackService.generateAiFollowUp(recording.getInterviewQuestion().getId());

                Optional.ofNullable(followUpQuestionText)
                        .filter(text -> !"추가 질문이 필요하지 않습니다.".equals(text))
                        .map(text -> interviewQuestionRepository.save(
                                InterviewQuestion.builder()
                                        .question(text)
                                        .interviewSession(recording.getInterviewQuestion().getInterviewSession())
                                        .parentQuestion(recording.getInterviewQuestion())
                                        .build()
                        ))
                        .ifPresent(recording.getInterviewQuestion()::attachFollowUp);

                statusService.setStatus(recordingId, RecordingStatus.FOLLOWUP_GENERATED, Duration.ofDays(1));

            } else {
                statusService.setStatus(recordingId, RecordingStatus.FAILED, Duration.ofMinutes(30));
            }
        } catch (Exception e) {
            statusService.setStatus(recordingId, RecordingStatus.FAILED, null);
            log.error("STT 워커 실행 실패 recordingId={}", recordingId, e); // 내부 로그
            throw new RuntimeException("STT 워커 실행 실패", e);
        }
    }

}