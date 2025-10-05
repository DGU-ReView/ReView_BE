package com.dgu.review.domain.interview.service;

import com.dgu.review.domain.interview.entity.InterviewQuestion;
import com.dgu.review.domain.interview.entity.Recording;
import com.dgu.review.domain.interview.entity.RecordingStatus;
import com.dgu.review.domain.interview.repository.InterviewQuestionRepository;
import com.dgu.review.domain.interview.repository.RecordingRepository;
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

//    @Value("${stt.worker.base-url}")
//    private String workerBaseUrl;
//
//    @Value("${app.s3.bucket}")
//    private String s3Bucket;

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

                statusService.setStatus(recording.getId(), RecordingStatus.FOLLOWUP_GENERATED, Duration.ofDays(1));

            } else {
                statusService.setStatus(recording.getId(), RecordingStatus.FAILED, Duration.ofMinutes(30));
            }
        } catch (Exception e) {
            statusService.setStatus(recording.getId(), RecordingStatus.UPLOADED, null);
            log.error("STT 워커 실행 실패 recordingId={}", recording.getId(), e); // 내부 로그
            throw new RuntimeException("STT 워커 실행 실패", e);
        }
    }

}