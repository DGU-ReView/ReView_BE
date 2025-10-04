package com.dgu.review.domain.interview.service;

import com.dgu.review.domain.interview.dto.SessionResultsResponse;
import com.dgu.review.domain.interview.dto.SttEnqueueResponse;
import com.dgu.review.domain.interview.dto.SttJobDetailResponse;
import com.dgu.review.domain.interview.entity.Recording;
import com.dgu.review.domain.interview.entity.RecordingStatus;
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
import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class SttService {

    private final RecordingRepository recordingRepo;
    private final RecordingStatusService statusService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${stt.worker.base-url}")
    private String workerBaseUrl;

    @Value("${aws.s3.bucket}")
    private String s3Bucket;

    @Transactional
    public SttEnqueueResponse enqueue(Long recordingId) {
        Recording r = recordingRepo.findById(recordingId)
                .orElseThrow(() -> {
                    log.warn("녹음을 찾을 수 없습니다. recordingId={}", recordingId); // 내부 로그
                    return new ApiException(ErrorCode.RECORDING_NOT_FOUND);
                });

        RecordingStatus current = statusService.getStatus(recordingId);
        if (current == RecordingStatus.TRANSCRIBING || current == RecordingStatus.COMPLETED) {
            return new SttEnqueueResponse(current.name());
        }

        statusService.setStatus(r.getId(), RecordingStatus.TRANSCRIBING);
        sttAsyncWorker(r); // 비동기

        return new SttEnqueueResponse(RecordingStatus.TRANSCRIBING.name());
    }



    /**
     * Whisper large-v3를 실행하는 비동기 워커
     */
    @Async
    public void sttAsyncWorker(Recording recording) {
        try {
            String projectRoot = System.getProperty("user.dir");
            String audioPath = projectRoot + File.separator + "audio_files" + File.separator + recording.getObjectKey();
            String scriptPath = projectRoot + File.separator + "stt_worker" + File.separator + "transcribe.py";

            ProcessBuilder pb = new ProcessBuilder("python", scriptPath, audioPath);
            pb.directory(new File(projectRoot));
            pb.redirectErrorStream(true);

            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                // 결과 텍스트 저장
                recordingRepo.updateSttTextById(recording.getId(), output.toString());
                // 상태 COMPLETE 저장
                statusService.setStatus(recording.getId(), RecordingStatus.COMPLETED);
            } else {
                // 실패 → 재시도 가능하도록 다시 UPLOADED 상태로
                statusService.setStatus(recording.getId(), RecordingStatus.UPLOADED);
            }
        } catch (Exception e) {
            statusService.setStatus(recording.getId(), RecordingStatus.UPLOADED);
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



    //세션 단위 polling: 세션 안의 녹음 조회하고 상태 받아서 리스트 반환하기
    @Transactional
    public SessionResultsResponse getSessionResults(Long sessionId) {
        var recordings = recordingRepo.findAllByInterviewQuestion_InterviewSession_Id(sessionId);
        if (recordings.isEmpty()) {
            log.warn("세션에 녹음이 존재하지 않습니다. sessionId={}", sessionId); // 내부 로그
            throw new ApiException(ErrorCode.SESSION_RECORDINGS_NOT_FOUND);
        }

        var items = recordings.stream().map(r -> {
            var status = statusService.getStatus(r.getId());
            var text   = (status == RecordingStatus.COMPLETED) ? r.getSttText() : null;
            return new SessionResultsResponse.Item(r.getId(), status, text);
        }).toList();

        // 집계 규칙 동일
        // 중복 제거 -> aggregate 메소드 사용
        String overall = aggregate(items);

        return new SessionResultsResponse(sessionId, overall, items);
    }
    // 전체 상태 집계 규칙: 모든 녹음이 COMPLETE일떄만 COMPLETE고, 하나라도
    private String aggregate(List<SessionResultsResponse.Item> items) {
        boolean allComplete = items.stream()
                .allMatch(i -> i.status() == RecordingStatus.COMPLETED);
        boolean anyTranscribing = items.stream()
                .anyMatch(i -> i.status() == RecordingStatus.TRANSCRIBING);

        if (allComplete) return RecordingStatus.COMPLETED.name();
        if (anyTranscribing) return RecordingStatus.TRANSCRIBING.name();
        return RecordingStatus.UPLOADED.name();
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