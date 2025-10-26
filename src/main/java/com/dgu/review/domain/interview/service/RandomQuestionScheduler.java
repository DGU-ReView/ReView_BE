package com.dgu.review.domain.interview.service;

import com.dgu.review.domain.interview.entity.InterviewQuestion;
import com.dgu.review.domain.peerfeedback.entity.PeerFeedback;
import com.dgu.review.domain.peerfeedback.repository.PeerFeedbackRepository;
import com.dgu.review.global.exception.ApiException;
import com.dgu.review.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
public class RandomQuestionScheduler {

    private final RandomNotificationService randomNotificationService;
    private final Random random = new Random();
    private final PeerFeedbackRepository peerFeedbackRepository;

    @Value("${notification.random.probability}")
    private int notificationRandomProbability;

    @Scheduled(fixedRate = 60000)
    @Transactional(readOnly = true)
    public void sendRandomQuestionNotification() {
        var activeUserIds = randomNotificationService.getActiveUserIds();

        for (Long userId : activeUserIds) {
            if (random.nextInt(100) < notificationRandomProbability) {

                List<Long> eligiblePeerIds = peerFeedbackRepository.findEligiblePeerFeedbackIdsForUser(userId);

                if (eligiblePeerIds.isEmpty()) {
                    log.warn("[Scheduler] User {} has no eligible random questions left.", userId);
                    continue;
                }
                Long randomPeerFeedbackId = eligiblePeerIds.get(random.nextInt(eligiblePeerIds.size()));

                PeerFeedback peerFeedback = peerFeedbackRepository.findById(randomPeerFeedbackId)
                                .orElseThrow(() -> new ApiException(ErrorCode.PEER_FEEDBACK_NOT_FOUND));

                if (peerFeedback == null) {
                    log.warn("[Scheduler] Cannot find PeerFeedback by id {}. Skipping.", randomPeerFeedbackId);
                    continue;
                }

                InterviewQuestion interviewQuestion = peerFeedback.getRecording().getInterviewQuestion();

                log.info("[Scheduler] Sending peerFeedbackId {} notification to user {}.", randomPeerFeedbackId, userId);


                randomNotificationService.sendNotification(
                        userId,
                        interviewQuestion.getInterviewSession().getJobRole(),
                        interviewQuestion.getInterviewSession().getTitle(),
                        interviewQuestion.getQuestionNumber(),
                        randomPeerFeedbackId
                );

            }
        }
    }

}
