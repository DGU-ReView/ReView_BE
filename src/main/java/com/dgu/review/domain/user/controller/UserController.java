package com.dgu.review.domain.user.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dgu.review.domain.user.service.GetUserService;
import com.dgu.review.global.exception.ApiException;
import com.dgu.review.global.exception.ErrorCode;
import com.dgu.review.global.security.CustomUserDetails;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    @GetMapping("/user/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
        	throw new ApiException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        Map<String, Object> result = Map.of(
            "userId", userDetails.getUserId(),
            "username", userDetails.getUsername(),
            "email", userDetails.getEmail(),
            "kakaoId", userDetails.getKakaoId()
        );

        return ResponseEntity.ok(result);
    }
    
    
}
