package com.dgu.review.domain.interview.entity;

import com.dgu.review.domain.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "interview_question")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewQuestion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TINYINT")
    private Integer questionNumber;

    @Column(nullable = false, columnDefinition = "text")
    private String question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_session_id", nullable = false)
    private InterviewSession interviewSession;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_question_id")
    private InterviewQuestion parentQuestion;

    @OneToOne(mappedBy = "parentQuestion", cascade = CascadeType.ALL, orphanRemoval = false, fetch = FetchType.EAGER)
    private InterviewQuestion followUpQuestion;

    @OneToOne(mappedBy = "interviewQuestion", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Recording recording;

    public void attachRecording(Recording recording) {
        this.recording = recording;
        if (recording != null) {
            recording.attachToQuestion(this);
        }
    }

    public void attachFollowUp(InterviewQuestion followUpQuestion) {
        this.followUpQuestion = followUpQuestion;
    }
    //follow
}
