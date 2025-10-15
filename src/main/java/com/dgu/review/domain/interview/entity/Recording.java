package com.dgu.review.domain.interview.entity;

import com.dgu.review.domain.common.entity.BaseEntity;
import com.dgu.review.domain.peerfeedback.entity.PeerFeedback;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "recording")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Recording extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 1024)
    private String objectKey;

    @Column(columnDefinition = "text", nullable = false)
    private String sttText;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_question_id", nullable = false)
    private InterviewQuestion interviewQuestion;

    @OneToMany(mappedBy = "recording", fetch = FetchType.LAZY, orphanRemoval = true)
    private List<PeerFeedback> peerFeedbacks = new ArrayList<>();

    public void attachToQuestion(InterviewQuestion question) {
        this.interviewQuestion = question;
        if (question != null && question.getRecording() != this) {
            question.attachRecording(this);
        }
    }

    public void attachSttText(String sttText) {
        this.sttText = sttText;
    }

    public void updateFailedAt(LocalDateTime failedAt) {
        this.failedAt = failedAt;
    }

}
