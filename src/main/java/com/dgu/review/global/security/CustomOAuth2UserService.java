package com.dgu.review.global.security;

import java.util.Collections;
import java.util.Map;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dgu.review.domain.user.entity.User;
import com.dgu.review.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 카카오에서 제공하는 기본 사용자 정보
        OAuth2User oAuth2User = super.loadUser(userRequest);

        Map<String, Object> attributes = oAuth2User.getAttributes();
        String kakaoId = String.valueOf(attributes.get("id"));
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");

        String email = null;
        if (kakaoAccount != null) {
            email = (String) kakaoAccount.get("email");
        }

        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
        
        //// 나중에 Name 받아오는걸로 변경 
        String username = (String) profile.get("nickname");

        // User 엔티티로 업서트
        User user = upsertUser(kakaoId, email, username);

        // UserDetails로 반환할 OAuth2User 객체 생성
        return new DefaultOAuth2User(
        	Collections.emptySet(),
            attributes,
            "id"
        );
    }

    @Transactional
    public User upsertUser(String kakaoId, String email, String username) {
        // 카카오 ID를 기준으로 사용자 찾기
        return userRepository.findByKakaoId(kakaoId)
            .map(existingUser -> {
                // 기존 사용자는 프로필 갱신
                existingUser.updateProfile(email, username);
                return existingUser;
            })
            .orElseGet(() -> {
                // 신규 사용자는 생성
                User newUser = User.builder()
                        .kakaoId(kakaoId)
                        .email(email)
                        .username(username)
                        .build();
                return userRepository.save(newUser);
            });
    }
}

