package com.dgu.review.domain.interview.service;

import com.dgu.review.domain.peerfeedback.repository.PeerFeedbackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
public class RandomQuestionScheduler {

    private final RandomNotificationService randomNotificationService;
    private final Random random = new Random();
    private final PeerFeedbackRepository peerFeedbackRepository;

    @Scheduled(fixedRate = 60000)
    public void sendRandomQuestionNotification() {
        var activeUserIds = randomNotificationService.getActiveUserIds();

        for (Long userId : activeUserIds) {
            if (random.nextInt(100) < 10) {

                List<Long> eligiblePeerIds = peerFeedbackRepository.findEligiblePeerFeedbackIdsForUser(userId);

                if (eligiblePeerIds.isEmpty()) {
                    log.warn("[Scheduler] User {} has no eligible random questions left.", userId);
                    return;
                }
                Long randomPeerFeedbackId = eligiblePeerIds.get(random.nextInt(eligiblePeerIds.size()));

                log.info("[Scheduler] Sending peerFeedbackId {} notification to user {}.", randomPeerFeedbackId, userId);
                randomNotificationService.sendNotification(userId, randomPeerFeedbackId);

            }
        }
    }

}
