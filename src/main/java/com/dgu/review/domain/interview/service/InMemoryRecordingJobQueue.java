package com.dgu.review.domain.interview.service;

import com.dgu.review.domain.interview.entity.Recording;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
@RequiredArgsConstructor
public class InMemoryRecordingJobQueue implements RecordingJobQueue {

    private final SttService sttService;

    @Override
    public void enqueue(Recording recording) {
        sttService.sttAsyncWorker(recording);
    }
}
