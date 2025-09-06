package com.dgu.review.domain.interview.entity;

import com.dgu.review.domain.common.entity.BaseEntity;
import com.dgu.review.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "interview_session")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewSession extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100)
    private String title;

    @Column(length = 1024, nullable = false)
    private String resumeObjectKey;

    @Column(length = 100)
    private String jobRole;

    //private User user;

    @OneToMany(mappedBy = "interviewSession", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InterviewQuestion> questions = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
