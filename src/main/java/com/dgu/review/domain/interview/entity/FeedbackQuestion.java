package com.dgu.review.domain.interview.entity;

import com.dgu.review.domain.peerfeedback.entity.PeerFeedback;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "feedback_question")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "text")
    private String question;

    @Column(columnDefinition = "text", name = "ai_feedback")
    private String aiFeedback;

    @Column(columnDefinition = "text", name = "self_feedback")
    private String selfFeedback;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_session_id", nullable = false)
    private InterviewSession interviewSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_question_id")
    private InterviewQuestion parentQuestion;

    @OneToOne(mappedBy = "feedbackQuestion", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Recording recording;

    @OneToOne(mappedBy = "generatedQuestion", fetch = FetchType.LAZY)
    private PeerFeedback sourcePeerFeedback;

    public void attachRecording(Recording recording) {
        this.recording = recording;
        if (recording != null) {
            recording.attachToFeedbackQuestion(this);
        }
    }

    public void attachAiFeedback(String aiFeedback) {
        this.aiFeedback = aiFeedback;
    }

    public void attachSelfFeedback(String selfFeedback) {
        this.selfFeedback = selfFeedback;
    }
}
