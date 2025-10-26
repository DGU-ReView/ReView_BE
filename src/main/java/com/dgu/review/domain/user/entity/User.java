package com.dgu.review.domain.user.entity;

import com.dgu.review.domain.common.entity.BaseEntity;
import com.dgu.review.domain.community.entity.CommunityPage;
import com.dgu.review.domain.interview.entity.InterviewSession;
import com.dgu.review.domain.peerfeedback.entity.PeerFeedback;
import com.dgu.review.domain.user.converter.ExperienceTagSetConverter;
import com.dgu.review.domain.user.converter.GrowthTagSetConverter;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
/*
  User 테이블 매핑
 */
import java.util.Set;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users",
uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_email", columnNames = "email")
})
public class User extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="kakao_id", nullable=false, length=20, unique = true)
    private String kakaoId;

    @Column(length = 255)
    private String email;

    @Column (length = 20)
    private String username;
    
    @Convert(converter = ExperienceTagSetConverter.class)
    @Column(name = "experience_tags", columnDefinition = "TEXT")
    @Builder.Default
    private Set<ExperienceTag> experienceTags = EnumSet.noneOf(ExperienceTag.class);

    @Convert(converter = GrowthTagSetConverter.class)
    @Column(name = "growth_tags", columnDefinition = "TEXT")
    @Builder.Default
    private Set<GrowthTag> growthTags = EnumSet.noneOf(GrowthTag.class);


    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InterviewSession> interviewSessions = new ArrayList<>();

    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommunityPage> communityPages = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PeerFeedback> peerFeedbacks = new ArrayList<>();
    
    public void updateProfile(String email, String name) {
        this.email = email;
        this.username = name;
    }
}