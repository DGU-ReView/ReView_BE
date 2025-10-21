package com.dgu.review.domain.interview.service;

import com.dgu.review.domain.user.service.GetUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class RandomNotificationService {

    private final Map<Long, SseEmitter> emitterMap = new ConcurrentHashMap<>();
    private final GetUserService getUserService;

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(10L * 60 * 1000);
        Long userId = getUserService.getUserId();

        emitterMap.put(userId, emitter);

        emitter.onCompletion(() -> emitterMap.remove(userId));
        emitter.onTimeout(() -> emitterMap.remove(userId));

        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("SSE connection established for user: " + userId));
        } catch (IOException e) {
            emitterMap.remove(userId);
        }

        return emitter;
    }

    public void sendNotification(Long userId, Long peerFeedbackId) {
        SseEmitter emitter = emitterMap.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("random-peer-feedback-id")
                        .data(peerFeedbackId));
            } catch (IOException e) {
                emitterMap.remove(userId);
            }
        }
    }

    public Set<Long> getActiveUserIds() {
        return emitterMap.keySet();
    }

}
