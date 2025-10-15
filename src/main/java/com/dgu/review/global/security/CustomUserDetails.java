package com.dgu.review.global.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.Getter;

import java.util.Collection;
import java.util.Collections;

@Getter
public class CustomUserDetails implements UserDetails {

    private String userId;
    private String username;
    private String email;
    private String kakaoId;
    private Collection<? extends GrantedAuthority> authorities;  // 권한

    // User 객체를 기반으로 CustomUserDetails 객체 생성
    public CustomUserDetails(String userId, String username, String email, String kakaoId) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.authorities = Collections.singletonList(() -> "ROLE_USER"); // 기본 권한 설정
        this.kakaoId=kakaoId;
    }
    
    // 밑에 모든 메서드는 필수 
    
    
    // 사용자의 권한을 반환
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;  
    }

    //비밀번호를 반환 (JWT에서는 비밀번호가 없지만, Spring Security는 이 메서드를 호출)
    @Override
    public String getPassword() {
        return null;  
    }

    //사용자 이름을 반환
    @Override
    public String getUsername() {
        return username;
    }

    // 계정 만료 여부 반환
    @Override
    public boolean isAccountNonExpired() {
        return true;  // 계정이 만료되지 않았으면 true 반환
    }

    // 계정 잠금 여부 반환
    @Override
    public boolean isAccountNonLocked() {
        return true;  // 계정이 잠기지 않았으면 true 반환
    }

    // 비밀번호 만료 여부 반환
    @Override
    public boolean isCredentialsNonExpired() {
        return true;  // 비밀번호가 만료되지 않았으면 true 반환
    }

    // 계정 활성화 여부 반환
    @Override
    public boolean isEnabled() {
        return true;  // 계정이 활성화되어 있으면 true 반환
    }
}
