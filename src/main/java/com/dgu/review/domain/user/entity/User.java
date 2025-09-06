package com.dgu.review.domain.user.entity;

import com.dgu.review.domain.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
/*
  User 테이블 매핑
 */

@Getter // 모든 필드에 getter 자동 생성함.
@NoArgsConstructor(access = AccessLevel.PROTECTED) //기본 생성자를 생성해줌. 필드값 없으면 객체 생성 막아준다.
@AllArgsConstructor // 전체 필드가 들어갈수 있도록 전체 생성자 생성
@Builder //User.builder().email(...).name(...).build() 형태로 생성., 필요한 필드에만 값을 넣어줄 수 있도록 해준다.

@Entity
@Table(name = "users",
uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_email", columnNames = "email"),
        @UniqueConstraint(name = "uk_users_name", columnNames = "name")
})
public class User extends BaseEntity {
    @Id //PK생성
    @GeneratedValue(strategy = GenerationType.IDENTITY) //AUTO_INCREMENT
    private Long id;

    @Column(nullable = false, length = 255)
    private String email;

}