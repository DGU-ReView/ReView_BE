package com.dgu.review.domain.community.entity;

import com.dgu.review.domain.common.entity.BaseEntity;
import com.dgu.review.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

/*
    community_page와 매핑
 */

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "community_page")
public class CommunityPage extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "page_id")
    private Long id;

    @Column(name = "title",nullable = false, length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "domain", nullable = false, length = 255)
    private DomainCategory domain;

    @Column(name = "company_name", nullable = false, length = 255)
    private String companyName;

    @Column(name = "job",nullable = false, length = 255)
    private String job;

    @Column(name = "interview_preps",columnDefinition ="MEDIUMTEXT",nullable = false)
    private String interviewPreps;

    @Column(name = "answer_strategies", columnDefinition = "MEDIUMTEXT", nullable = false)
    private String answerStrategies;

    @Column(name = "tips", columnDefinition = "MEDIUMTEXT", nullable = false)
    private String tips;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)//user 전체 행을 가져오지 않고 호출 시 가져옴(<->EAGER. 연관 관계 필수 지정.
    @JoinColumn(name = "author_id", nullable = false,
    foreignKey = @ForeignKey(name = "fk_cp_author")) //외래키 제약 이름 지정
    private User author; //객체 연관관계를 테이블의 외래키로 바꿔서 저장/조회

    public void updateContents(String interviewPreps, String answerStrategies, String tips) {
        this.interviewPreps = interviewPreps;
        this.answerStrategies = answerStrategies;
        this.tips = tips;
    }
}