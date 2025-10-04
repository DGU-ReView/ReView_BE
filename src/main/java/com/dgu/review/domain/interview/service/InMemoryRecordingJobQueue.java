package com.dgu.review.domain.interview.service;

import com.dgu.review.domain.interview.entity.Recording;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
@RequiredArgsConstructor
public class InMemoryRecordingJobQueue implements RecordingJobQueue {

    private final RecordingTranscriber recordingTranscriber;

    @Override
    public void enqueue(Recording recording) {
        recordingTranscriber.sttAsyncWorker(recording);
    }
}
