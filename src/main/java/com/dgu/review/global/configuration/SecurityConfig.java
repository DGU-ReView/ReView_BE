package com.dgu.review.global.configuration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
public class SecurityConfig {
	
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            AuthenticationSuccessHandler oAuth2SuccessHandler,
                                            AuthenticationFailureHandler oAuth2FailureHandler,
                                            AuthenticationEntryPoint restAuthEntryPoint
                                           ) throws Exception {

        return http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/", "/health", "/favicon.ico", "/error",
                    "/oauth2/authorization/kakao",
                    "/login/oauth2/code/kakao"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(e -> e
                    .defaultAuthenticationEntryPointFor(restAuthEntryPoint, new AntPathRequestMatcher("/api/**"))
                    .defaultAccessDeniedHandlerFor(restAccessDeniedHandler(), new AntPathRequestMatcher("/api/**"))
                )
            .oauth2Login(oauth -> oauth
                .successHandler(oAuth2SuccessHandler)
                .failureHandler(oAuth2FailureHandler)
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/")
            )
            .build();
    }

    // ===== 성공/실패 핸들러 최소 구현 =====

//    @Bean
//    AuthenticationSuccessHandler oAuth2SuccessHandler() {
//        return (HttpServletRequest req, HttpServletResponse res, Authentication auth) -> {
//            OAuth2User principal = (OAuth2User) auth.getPrincipal();
//
//            // kakao 고유 id
//            String kakaoId = String.valueOf(principal.getAttribute("id"));
//
//            // TODO: 여기서 DB 업서트(없으면 생성/있으면 갱신)
//            // TODO: 세션 사용 시: 아무것도 안 해도 인증 완료 (스프링 시큐리티 세션)
//            // TODO: JWT 사용 시: 우리 서비스용 JWT 발급 후 쿠키에 심기
//
//            // 최소: 프론트 성공 페이지로 리다이렉트
//            // (로컬 프론트가 3000이라 가정. 필요에 맞게 변경)
//            res.sendRedirect("http://localhost:3000/login/success");
//        };
//    }

//    @Bean
//    AuthenticationFailureHandler oAuth2FailureHandler() {
//        return (HttpServletRequest req, HttpServletResponse res, Exception ex) -> {
//            // 실패 로그 남기고 에러 페이지/쿼리 파라미터로 리다이렉트
//            res.sendRedirect("/login?error=" + (ex.getMessage() == null ? "oauth2_failed" : ex.getMessage()));
//        };
//    }
    
    // 401(인증) JSON 응답용
    @Bean
    AuthenticationEntryPoint restAuthEntryPoint() {
        return (HttpServletRequest req, HttpServletResponse res, AuthenticationException ex) -> {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED); 
            res.setContentType("application/json;charset=UTF-8");
            res.getWriter().write("""
                {"error":"unauthorized","message":"로그인이 필요합니다."}
            """);
        };
    }
    // 403(인가) JSON 응답용
    @Bean
    AccessDeniedHandler restAccessDeniedHandler() {
        return (req, res, ex) -> {
            res.setStatus(HttpServletResponse.SC_FORBIDDEN);
            res.setContentType("application/json;charset=UTF-8");
            res.getWriter().write("""
                {"error":"forbidden","message":"권한이 없습니다."}
            """);
        };
    }

}