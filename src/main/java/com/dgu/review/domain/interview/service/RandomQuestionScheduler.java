package com.dgu.review.domain.interview.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
@RequiredArgsConstructor
public class RandomQuestionScheduler {

    private final RandomNotificationService randomNotificationService;
    private final Random random = new Random();

    @Scheduled(fixedRate = 60000)
    public void sendRandomQuestionNotification() {
        var activeUserIds = randomNotificationService.getActiveUserIds();

        for (Long userId : activeUserIds) {
            if (random.nextInt(100) < 10) {
                // 랜덤 질문 가져오는 로직: 랜덤 service에서 생성, id만 보내줌
                Long randomPeerFeedbackId = 1L;

                randomNotificationService.sendNotification(userId, randomPeerFeedbackId);

            }
        }
    }

}
