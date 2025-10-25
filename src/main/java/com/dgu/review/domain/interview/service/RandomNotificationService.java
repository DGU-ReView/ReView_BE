package com.dgu.review.domain.interview.service;

import com.dgu.review.domain.interview.dto.response.PeerFeedbackNotification;
import com.dgu.review.domain.user.service.GetUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class RandomNotificationService {

    private final Map<Long, SseEmitter> emitterMap = new ConcurrentHashMap<>();
    private final GetUserService getUserService;
    private final ObjectMapper objectMapper;

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

    public void sendNotification(Long userId, String jobName, String interviewName, int questionNumber, Long peerFeedbackId) {
        SseEmitter emitter = emitterMap.get(userId);
        if (emitter != null) {
            try {
                PeerFeedbackNotification payload = new PeerFeedbackNotification(
                        jobName,
                        interviewName,
                        questionNumber,
                        peerFeedbackId
                );

                String jsonPayload = objectMapper.writeValueAsString(payload);

                emitter.send(SseEmitter.event()
                        .name("random-peer-feedback")
                        .data(jsonPayload));
            } catch (IOException e) {
                log.warn("[SSE Send Error] Failed to send notification to user {}: {}", userId, e.getMessage());
                emitterMap.remove(userId);
            }
        } else {
            log.warn("[SSE Send] No emitter found for user {}. Notification skipped.", userId);
        }
    }

    public Set<Long> getActiveUserIds() {
        return emitterMap.keySet();
    }

}
