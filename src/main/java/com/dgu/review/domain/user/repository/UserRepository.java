package com.dgu.review.domain.user.repository;

import com.dgu.review.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
