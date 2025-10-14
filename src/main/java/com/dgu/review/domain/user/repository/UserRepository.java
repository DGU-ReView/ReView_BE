package com.dgu.review.domain.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dgu.review.domain.user.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

}
