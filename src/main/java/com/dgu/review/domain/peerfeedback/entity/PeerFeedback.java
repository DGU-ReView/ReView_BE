package com.dgu.review.domain.peerFeedback.entity;

import com.dgu.review.domain.common.entity.BaseEntity;
import com.dgu.review.domain.interview.entity.Recording;
import com.dgu.review.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

/*
    peer_feedback 테이블 매핑
 */

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "peer_feedback")
public class PeerFeedback extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String body;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recording_id", nullable = false,
    foreignKey = @ForeignKey(name = "fk_pf_recording"))
    private Recording recording;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
    foreignKey = @ForeignKey(name = "fk_pf_user"))
    private User user;


}