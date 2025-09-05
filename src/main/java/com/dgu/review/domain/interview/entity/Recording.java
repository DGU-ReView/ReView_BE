package com.dgu.review.domain.interview.entity;

import com.dgu.review.domain.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_question_id", nullable = false)
    private InterviewQuestion interviewQuestion;

    @OneToOne(mappedBy = "recording", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private AiFeedback aiFeedback;
}
