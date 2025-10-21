package com.dgu.review.domain.interview.service;

import com.dgu.review.domain.interview.entity.Recording;

public interface RecordingJobQueue {
    void enqueue(Long recordingId);

    void enqueueForRandom(Long recordingId);
}
