package com.dgu.review.domain.interview.service;

import com.dgu.review.domain.interview.dto.SttEnqueueResponse;
import com.dgu.review.domain.interview.dto.SttJobDetailResponse;
import com.dgu.review.domain.interview.entity.Recording;
import com.dgu.review.domain.interview.repository.RecordingRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.nio.charset.StandardCharsets;



@Service
@RequiredArgsConstructor
public class SttService {

    private final RecordingRepository recordingRepo;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${stt.worker.base-url:http://localhost:8081}")
    private String workerBaseUrl;

    @Value("${app.s3.bucket:your-audio-bucket}")
    private String s3Bucket;

    @Transactional
    public SttEnqueueResponse enqueue(Long recordingId) {
        Recording r = recordingRepo.findById(recordingId)
                .orElseThrow(() -> new IllegalArgumentException("Recording not found"));

        String status = deriveStatus(r);
        if ("TRANSCRIBING".equals(status) || "SUCCEEDED".equals(status)) {
            return new SttEnqueueResponse(r.getId(), status);
        }
        // FAILED 상태도 재시도 가능
        // UPLOADED 상태여야 시작 가능

        // 1) 상태를 TRANSCRIBING으로 (setter 없이 JPQL update)
        recordingRepo.updateSttTextById(r.getId(), "__TRANSCRIBING__");

        // 2) 내부 비동기 워커 실행 (Whisper large-v3)
        sttAsyncWorker(r);

        // 3) 외부 워커 호출 (faster-whisper large-v3) -> 추후 AWS Lambda 연동 시 사용
    /*
    SttWorkerRequest payload = new SttWorkerRequest(
            r.getId(),
            s3Bucket,
            r.getObjectKey(),
            "ko",
            "large-v3",
            false
    );
    try {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<SttWorkerRequest> entity = new HttpEntity<>(payload, headers);
        restTemplate.exchange(
                workerBaseUrl + "/worker/transcribe",
                HttpMethod.POST, entity, String.class
        );
    } catch (Exception e) {
        String errorMessage = "Worker dispatch failed: " + e.getMessage();
        recordingRepo.updateSttTextById(r.getId(), errorMessage);
        throw new IllegalStateException(errorMessage, e);
    }
    */

        return new SttEnqueueResponse(r.getId(), "TRANSCRIBING");
    }


    /**
     * Whisper large-v3를 실행하는 비동기 워커
     */
    @Async
    public void sttAsyncWorker(Recording recording) {
        try {
            // 현재 실행 중인 프로젝트 루트 경로 가져오기
            String projectRoot = System.getProperty("user.dir");

            // 오디오 파일 경로
            String audioPath = projectRoot + File.separator + "audio_files" + File.separator + recording.getObjectKey();

            // 파이썬 스크립트 경로
            String scriptPath = projectRoot + File.separator + "stt_worker" + File.separator + "transcribe.py";

            ProcessBuilder pb = new ProcessBuilder(
                    "python", scriptPath, audioPath
            );
            pb.directory(new File(projectRoot)); // 실행 디렉토리를 프로젝트 루트로 고정
            pb.redirectErrorStream(true);

            Process process = pb.start();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
            );
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                recordingRepo.updateSttTextById(recording.getId(), output.toString());
            } else {
                recordingRepo.updateSttTextById(recording.getId(), "FAILED: Whisper 실행 오류");
            }
        } catch (Exception e) {
            recordingRepo.updateSttTextById(recording.getId(), "FAILED: " + e.getMessage());
        }
    }



    @Transactional
    public void complete(Long recordingId, String transcript) {
        // 워커 완료 콜백에서 최종 텍스트 반영
        recordingRepo.updateSttTextById(recordingId, transcript == null ? "" : transcript);
    }

    @Transactional
    public SttJobDetailResponse getDetail(Long recordingId) {
        Recording r = recordingRepo.findById(recordingId)
                .orElseThrow(() -> new IllegalArgumentException("Recording not found"));
        String status = deriveStatus(r);
        String text = "SUCCEEDED".equals(status) ? r.getSttText() : null;
        String error = "FAILED".equals(status) ? r.getSttText() : null;
        return new SttJobDetailResponse(r.getId(), status, text, null, error);
    }

    private String deriveStatus(Recording r) {
        String t = r.getSttText();
        if (t == null || t.isEmpty()) return "UPLOADED";
        if ("__TRANSCRIBING__".equals(t)) return "TRANSCRIBING";
        if (t.startsWith("Worker dispatch failed")) return "FAILED"; // 에러 메시지 패턴으로 구분
        return "SUCCEEDED";
    }

    // 워커에 넘기는 최소 페이로드
    public record SttWorkerRequest(
            Long recordingId, String s3Bucket, String objectKey,
            String lang, String model, boolean diarize
    ) {}
}

//enqueue: 녹음 파일을 STT 워커에 보내서 전사 시작 요청
//complete: 워커가 완료했을 때 결과 반영
//getDetail: 클라이언트가 조회할 때 상태/결과 반환
//deriveStatus: 상태