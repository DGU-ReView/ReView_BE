package com.dgu.review.domain.interview.controller;

import com.dgu.review.domain.interview.service.RandomNotificationService;
import com.dgu.review.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
public class RandomNotificationController {

    private final RandomNotificationService randomNotificationService;

    @GetMapping(value = "/api/subscribe", produces = "text/event-stream")
    public SseEmitter subscribe() {
        return randomNotificationService.subscribe();
    }
}
