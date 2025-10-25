package com.dgu.review.domain.interview.service;

import com.dgu.review.domain.interview.entity.Recording;
import com.dgu.review.domain.interview.entity.RecordingStatus;
import com.dgu.review.domain.interview.repository.RecordingRepository;
import com.dgu.review.global.exception.ApiException;
import com.dgu.review.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class SttExecutionService {

    private final RecordingStatusService statusService;
    private final RecordingRepository recordingRepository;

    public String executeTranscribe(Long recordingId, String audioPresignedUrl) throws Exception {

        Recording recording = recordingRepository.findById(recordingId)
                .orElseThrow(() -> new ApiException(ErrorCode.RECORDING_NOT_FOUND));

        long started = System.currentTimeMillis();

        log.info("[status:update] recordingId={} -> TRANSCRIBING (readback={})",
                recordingId, statusService.getStatus(recordingId));

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

        if (exitCode != 0) {
            statusService.setStatus(recordingId, RecordingStatus.FAILED, Duration.ofMinutes(30));
            recording.updateFailedAt(LocalDateTime.now());

            log.error("STT 워커 실행 실패 recordingId={} with exitCode {}", recordingId, exitCode);
            throw new RuntimeException("STT 워커 실행 실패 (exit code: " + exitCode + ")" );
        }
        return output.toString();
    }
}
