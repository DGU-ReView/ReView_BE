package com.dgu.review.domain.interview.controller;

import com.dgu.review.domain.interview.dto.SessionResultsResponse;
import com.dgu.review.domain.interview.service.SttService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


/*
인터뷰 녹음 파일에 대해 STT작업을 걸고 그 진행 상태 및 결과를 조회.
POST 이용해서 워커를 호출해달라고 명령, GET이용해서 결과가 나왔는지 확인.
전사 작업을 비동기로 등록하고 202 Accepted로 즉시 응답.
 */

@RestController
@RequestMapping("/api/interview-sessions")
@RequiredArgsConstructor
@Validated
public class SttController {

    private final SttService sttService;

    //세션 단위 polling API
    @GetMapping("/{sessionId}/results") // base path 뒤에만 붙임
    public ResponseEntity<SessionResultsResponse> getSessionStatus(@PathVariable Long sessionId) {
        return ResponseEntity.ok(sttService.getSessionResults(sessionId));
    }


    // 단일 레코드 단위 주석 처리함.
    /*
     * [전사 시작]
     * - recordingId가 UPLOADED 상태인지 검증
     * - Recording.status 를 TRANSCRIBING 으로 바꾸고
     * - 내부 워커 HTTP로 전송
     * - 즉시 202(ACCEPTED)반환
     */
    /*
       @PostMapping("/recordings/{recordingId}/transcribe")
    public ResponseEntity<SttEnqueueResponse> transcribe(
            @PathVariable Long recordingId

    ) {
        var res = sttService.enqueue(recordingId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(res); // 202: 처리 중
    }
     */


    /*
     * [전사 상태/결과 조회]
     * 현재 status(SUCCEEDED/FAILED/TRANSCRIBING)와
     *  sttText, resultSrtKey, error 등을 반환
     */

    /*
        @GetMapping("/recordings/{recordingId}/stt")
    public ResponseEntity<SttJobDetailResponse> getDetail(@PathVariable Long recordingId) {
        var res = sttService.getDetail(recordingId);
        return ResponseEntity.ok(res);
    }
     */
}