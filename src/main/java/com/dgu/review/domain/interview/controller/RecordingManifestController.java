// 녹음 파일 접수 api
/*
client가 S3에 음성 파일을 올려두고 그 키를 백엔드에 알려주면
DB에 레코드 행을 만들어줌.
 */

package com.dgu.review.domain.interview.controller;

import com.dgu.review.domain.interview.dto.RecordingManifestCreateRequest;
import com.dgu.review.domain.interview.dto.RecordingManifestCreateResponse;
import com.dgu.review.domain.interview.dto.RecordingManifestDetailResponse;
import com.dgu.review.domain.interview.service.RecordingManifestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

//파일이 어떤 상태인지 추적하고, DB에 그 메타데이터(버킷/경로/상태/길이 등)를 저장, 갱신

@RestController //api 요청을 받는 컨트롤러
@RequestMapping("/api/interview/recordings")
@RequiredArgsConstructor
@Validated //요청값 검증 가능
public class RecordingManifestController {

    private final RecordingManifestService manifestService;
    //실제 로직은 서비스에서 처리. 컨트롤러는 요청받고 서비스에 전달 후 응답반환 역할만 함.


    @PostMapping //POST 요청 받음.
    public ResponseEntity<RecordingManifestCreateResponse> create(
            @Valid @RequestBody RecordingManifestCreateRequest req // 요청 JSON을 객체로 변환
            //Valid: 값 검증.
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(manifestService.create(req));
        //manifestService.create(req) -> DB에 recoding 저장.
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecordingManifestDetailResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(manifestService.get(id));
    }
}
