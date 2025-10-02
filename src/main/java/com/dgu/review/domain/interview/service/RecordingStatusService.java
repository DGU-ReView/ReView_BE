package com.dgu.review.domain.interview.service;

import com.dgu.review.domain.interview.entity.RecordingStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RecordingStatusService {

    private final StringRedisTemplate redisTemplate;

    private String key(Long recordingId) {
        return "recording:" + recordingId + ":status";
    }

    // 상태 저장
    public void setStatus(Long recordingId, RecordingStatus status, @Nullable Duration ttl) {
        String key = key(recordingId);
        redisTemplate.opsForValue().set(key, status.name());
        if (ttl != null) redisTemplate.expire(key, ttl);
    }

    // 상태 조회
    public RecordingStatus getStatus(Long recordingId) {
        String status = redisTemplate.opsForValue().get(key(recordingId));
        return status != null ? RecordingStatus.valueOf(status) : null;
    }

    // 상태 삭제 (세션 종료 시 등 필요할 때 호출)
    public void clearStatus(Long recordingId) {
        redisTemplate.delete(key(recordingId));
    }

    // 최초 등록
    public boolean trySetUploadedIfAbsent(Long recordingId) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue()
                .setIfAbsent(key(recordingId), RecordingStatus.UPLOADED.name())
        );
    }
}
