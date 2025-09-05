package com.dgu.review.domain.peerfeedback.entity;

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

@Table(name = "peer_feedback")
public class PeerFeedback{
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String body;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recoding_id", nullable = false,
    foreignKey = @ForeignKey(name = "fk_pf_recoding"))
    private Recoding recoding;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
    foreignKey = @ForeignKey(name = "fk_pf_user"))
    private User user;
}