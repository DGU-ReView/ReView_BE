package com.dgu.review.domain.interview.service;

import com.dgu.review.domain.interview.entity.RecordingStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecordingStatusService {

    private final RedisTemplate<String, String> redisTemplate;

    private String key(Long recordingId) {
        return "recording:" + recordingId + ":status";
    }

    // 상태 저장
    public void setStatus(Long recordingId, RecordingStatus status) {
        redisTemplate.opsForValue().set(key(recordingId), status.name());
    }

    // 상태 조회
    public RecordingStatus getStatus(Long recordingId) {
        String status = redisTemplate.opsForValue().get(key(recordingId));
        return status != null ? RecordingStatus.valueOf(status) : RecordingStatus.UPLOADED;
    }

    // 상태 삭제 (세션 종료 시 등 필요할 때 호출)
    public void clearStatus(Long recordingId) {
        redisTemplate.delete(key(recordingId));
    }
}
