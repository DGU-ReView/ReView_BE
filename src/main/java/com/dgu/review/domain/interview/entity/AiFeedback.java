package com.dgu.review.domain.interview.entity;

import com.dgu.review.domain.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ai_feedback")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiFeedback extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "text")
    private String aiFeedback;

    @Column(columnDefinition = "text")
    private String peerCriteriaFeedback;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recording_id", nullable = false)
    private Recording recording;

}
