package com.dgu.review.domain.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dgu.review.domain.user.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
	User findUserByKakaoId(String kakaoId); 
	Optional<User> findByKakaoId(String kakaoId);
    boolean existsByKakaoId(String kakaoId);
}
