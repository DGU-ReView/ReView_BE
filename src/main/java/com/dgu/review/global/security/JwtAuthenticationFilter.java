package com.dgu.review.global.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import com.dgu.review.domain.user.entity.User;
import com.dgu.review.domain.user.repository.UserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@AllArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenUtil jwtTokenUtil;
    private final UserRepository userRepository;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        //토큰 추출
        String token = extractToken(request);

        if (token != null) {
            String kakaoId = jwtTokenUtil.extractKakaoId(token);

            // 토큰 유효성 확인
            if (jwtTokenUtil.validateToken(token, kakaoId)) {

                // User 조회 
            	User user = userRepository.findUserByKakaoId(kakaoId);
            	if (user == null) {
            	    throw new RuntimeException("User not found");
            	}

                // CustomUserDetails 생성
                CustomUserDetails userDetails = new CustomUserDetails(
                        user.getId(), 
                        user.getUsername(),           
                        user.getEmail(),           
                        kakaoId
                );

                // 인증 객체 생성 & 정보 설정 
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        //다음 필터로 요청 전달
        filterChain.doFilter(request, response);
    }


    // JWT 토큰 추출 (Authorization 헤더 또는 쿠키에서)
    private String extractToken(HttpServletRequest request) {
        String token = null;
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            token = header.substring(7);  
        }
        if (token == null) {
            // 쿠키에서 토큰을 추출 
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("access_token".equals(cookie.getName())) {
                        token = cookie.getValue();  
                        break;
                    }
                }
            }
        }
        
        return token;
    }
}
