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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
    private final FeedbackJob feedbackJob;
    private final SttExecutionService sttExecutionService;

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
        Recording recording = recordingRepo.findById(recordingId)
                .orElseThrow(() -> new ApiException(ErrorCode.RECORDING_NOT_FOUND));

        try {
            log.info("[sttWorker:start] recordingId={}, thread={}, audioKey={}",
                    recordingId, Thread.currentThread().getName(), recording.getObjectKey());

            statusService.setStatus(recordingId, RecordingStatus.TRANSCRIBING, null);

            String audioPresignedUrl = interviewObjectReadService.createRecordingGetUrl(recording.getObjectKey());

            String sttText = sttExecutionService.executeTranscribe(recordingId, audioPresignedUrl);

            recording.attachSttText(sttText);
            statusService.setStatus(recordingId, RecordingStatus.COMPLETED, null);

            String followUpQuestionText = sttFeedbackService.generateAiFollowUp(recording.getInterviewQuestion().getId());
            String normalized = normalize(followUpQuestionText);
            boolean createdFollowUp = Optional.ofNullable(followUpQuestionText)
                    .filter(text -> !normalize(text).equals("추가 질문이 필요하지 않습니다"))
                    .map(text -> interviewQuestionRepository.save(
                            InterviewQuestion.builder()
                                    .question(text)
                                    .interviewSession(recording.getInterviewQuestion().getInterviewSession())
                                    .parentQuestion(recording.getInterviewQuestion())
                                    .build()
                    ))
                    .map(saved -> {
                        recording.getInterviewQuestion().attachFollowUp(saved);
                        return true;
                    })
                    .orElse(false);

            log.info("[followup] questionId={} raw='{}' normalized='{}' created={}",
                    recording.getInterviewQuestion().getId(), safePreview(followUpQuestionText, 100), normalized, createdFollowUp);

            if (!createdFollowUp) {
                InterviewQuestion rootQ = null;
                try {
                    recordingRepo.flush();

                    rootQ = getRoot(recording.getInterviewQuestion());
                    Long rootId = rootQ.getId();

                    log.info("[feedback] will enqueue for rootId={} (from qId={}) path={}",
                            rootQ.getId(), recording.getInterviewQuestion().getId(), dumpPathIds(recording.getInterviewQuestion()));
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override public void afterCommit() {
                            log.info("[feedback] afterCommit enqueue rootId={}", rootId);
                            feedbackJob.generateAiThenSelfAsync(rootId);
                            //feedbackJob.generateAiFeedbackAsync(rootId);
                            //feedbackJob.generateSelfFeedbackAsync(rootId);
                        }
                    });

                } catch(Exception e) {
                    log.error("[feedback] async job failed, rootId={}", rootQ.getId(), e);
                }

                statusService.setStatus(recordingId, RecordingStatus.FOLLOWUP_GENERATED, Duration.ofDays(1));

            }
        } catch (Exception e) {
            statusService.setStatus(recordingId, RecordingStatus.FAILED, null);
            recording.updateFailedAt(LocalDateTime.now());
        }
    }

    @Transactional
    @Async
    public void sttAsyncWorkerForRandom(Long recordingId) {
        Recording recording = recordingRepo.findById(recordingId)
                .orElseThrow(() -> new ApiException(ErrorCode.RECORDING_NOT_FOUND));

        try {
            log.info("[SttWorkerForRandom:start] recordingId={}, thread={}, audioKey={}",
                    recordingId, Thread.currentThread().getName(), recording.getObjectKey());

            statusService.setStatus(recordingId, RecordingStatus.TRANSCRIBING, null);

            String audioPresignedUrl = interviewObjectReadService.createRecordingGetUrl(recording.getObjectKey());

            String sttText = sttExecutionService.executeTranscribe(recordingId, audioPresignedUrl);

            recording.attachSttText(sttText);
            statusService.setStatus(recordingId, RecordingStatus.COMPLETED, null);

            InterviewQuestion randomQuestion = recording.getInterviewQuestion();

            try {
                recordingRepo.flush();


                log.info("[feedback] will enqueue for randomQuestionId={} (from qId={}) path={}",
                        randomQuestion.getId(), recording.getInterviewQuestion().getId(), dumpPathIds(recording.getInterviewQuestion()));
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override public void afterCommit() {
                        log.info("[feedback] afterCommit enqueue randomQuestionId={}", randomQuestion.getId());
                        feedbackJob.generateAiThenSelfAsync(randomQuestion.getId());
                        //feedbackJob.generateAiFeedbackAsync(rootId);
                        //feedbackJob.generateSelfFeedbackAsync(rootId);
                    }
                });

            } catch(Exception e) {
                log.error("[feedback] async job failed, rootId={}", randomQuestion.getId(), e);
            }

            statusService.setStatus(recordingId, RecordingStatus.FOLLOWUP_GENERATED, Duration.ofDays(1));

        } catch (Exception e) {
            statusService.setStatus(recordingId, RecordingStatus.FAILED, null);
            recording.updateFailedAt(LocalDateTime.now());
        }
    }

    private InterviewQuestion getRoot(InterviewQuestion q) {
        var cur = q;
        int depth = 0;

        while (cur.getParentQuestion() != null) {
            cur = cur.getParentQuestion();

            if (++depth > 10) {
                log.error("질문 데이터 순환 참조 의심. recordingId: {}", q.getRecording().getId());
                throw new ApiException(ErrorCode.DATA_INTEGRITY_VIOLATED);
            }
        }
        return cur;
    }

    // 문자열 끝부분 따옴표, 공백, 마침표, 앞뒤 공백 제거
    private String normalize(String s) {
        if (s == null) return "";
        return s
                .replaceAll("[\"'\\s.]+$", "")
                .trim();
    }

    private String safePreview(String s, int max) {
        if (s == null) return "";
        s = s.replaceAll("\\s+", " ").trim();
        return (s.length() <= max) ? s : s.substring(0, max) + "...";
    }

    private String dumpPathIds(InterviewQuestion q) {
        try {
            List<Long> ids = new ArrayList<>();
            Optional<InterviewQuestion> currentOpt = Optional.of(q);
            int guard = 0;
            while (currentOpt.isPresent() && guard++ < 50) {
                InterviewQuestion current = currentOpt.get();
                ids.add(current.getId());

                InterviewQuestion parentProxy = current.getParentQuestion();

                if (parentProxy != null) {
                    currentOpt = interviewQuestionRepository.findById(parentProxy.getId());
                } else {
                    currentOpt = Optional.empty();
                }
            }
            return ids.toString();
        } catch (Exception e) {
            return "[error building path]";
        }
    }
}