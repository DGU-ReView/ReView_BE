package com.dgu.review.global.configuration;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;

import com.dgu.review.domain.user.entity.User;
import com.dgu.review.domain.user.repository.UserRepository;
import com.dgu.review.global.security.CookieUtils;
import com.dgu.review.global.security.CustomOAuth2UserService;
import com.dgu.review.global.security.HttpCookieOAuth2AuthorizationRequestRepository;
import com.dgu.review.global.security.JwtAuthenticationFilter;
import com.dgu.review.global.security.JwtTokenUtil;

@Configuration
@AllArgsConstructor
@Slf4j
public class SecurityConfig {

	private final JwtTokenUtil jwtTokenUtil;
	private final UserRepository userRepository;

	@Bean
	public HttpCookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository() {
		return new HttpCookieOAuth2AuthorizationRequestRepository();
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationSuccessHandler oAuth2SuccessHandler,
			AuthenticationFailureHandler oAuth2FailureHandler, AuthenticationEntryPoint restAuthEntryPoint,
			AccessDeniedHandler restAccessDeniedHandler, JwtTokenUtil jwtTokenUtil, UserRepository userRepository,
			HttpCookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository,
			CustomOAuth2UserService customOAuth2UserService) throws Exception {

		return http.csrf(csrf -> csrf.disable()).cors(cors -> cors.configurationSource(request -> {
			CorsConfiguration config = new CorsConfiguration();
			config.addAllowedOrigin("http://localhost:3000"); // 프론트 도메인 -> 수정 필요
			config.setAllowCredentials(true);
			config.addAllowedHeader("*");
			config.addAllowedMethod("*");
			return config;
		})).authorizeHttpRequests(auth -> auth
				.requestMatchers("/", "/health", "/favicon.ico", "/error", "/oauth2/authorization/kakao",
						"/login/oauth2/code/kakao", "/swagger-ui/**", "/v3/api-docs/**")
				.permitAll().anyRequest().authenticated())
				.exceptionHandling(e -> e
						.defaultAuthenticationEntryPointFor(restAuthEntryPoint, new AntPathRequestMatcher("/api/**"))
						.defaultAccessDeniedHandlerFor(restAccessDeniedHandler(), new AntPathRequestMatcher("/api/**")))
				.oauth2Login(oauth -> oauth
						.authorizationEndpoint(
								auth -> auth.authorizationRequestRepository(cookieAuthorizationRequestRepository))
						.userInfoEndpoint(u -> u.userService(customOAuth2UserService))
						.successHandler(oAuth2SuccessHandler).failureHandler(oAuth2FailureHandler))
				.logout(logout -> logout.logoutUrl("/logout")
						.logoutSuccessHandler((request, response, authentication) -> {
							// 로그아웃시 JWT 쿠키 삭제
							CookieUtils.deleteCookie(request, response, "access_token");
							// JSON 응답
							response.setStatus(HttpServletResponse.SC_OK);
							response.setContentType("application/json;charset=UTF-8");
							response.getWriter().write("{\"message\":\"로그아웃 성공\"}");
						}))
				.addFilterBefore(new JwtAuthenticationFilter(jwtTokenUtil, userRepository),
						UsernamePasswordAuthenticationFilter.class)
				.build();
	}

	// 성공 핸들러
	@Bean
	public AuthenticationSuccessHandler oAuth2SuccessHandler() {
		return (HttpServletRequest req, HttpServletResponse res, Authentication auth) -> {
			// 인증된 사용자 정보 가져오기
			OAuth2User oAuth2User = (OAuth2User) auth.getPrincipal();
			String kakaoId = oAuth2User.getAttribute("id").toString();
			User user = userRepository.findUserByKakaoId(kakaoId);
			if (user == null) {
				throw new RuntimeException("User not found");
			}

			// JWT 생성
			String token = jwtTokenUtil.generateToken(user.getId(), user.getKakaoId(), user.getUsername(),
					new HashMap<>());

			// 쿠키로 전달
			CookieUtils.addCookie(res, "access_token", token, 60 * 60 * 24); // 1일 만료

			// 성공 후 리다이렉트 // 프론트와 연동시 주석 해
//            res.sendRedirect("http://localhost:3000/login/success"); 

			// 프론트와 연동시 삭제
			// 200 OK + JSON 반환
			res.setContentType("application/json;charset=UTF-8");
			res.setStatus(HttpServletResponse.SC_OK);
			res.getWriter().write("{\"message\": \"로그인 성공(프론트와 연동시 삭제 )\", \"kakaoId\": \"" + kakaoId + "\"}");
		};
	}

	@Bean
	AuthenticationFailureHandler oAuth2FailureHandler() {
		return (HttpServletRequest req, HttpServletResponse res, AuthenticationException ex) -> {
			// 실패 로그
			log.error("🚨OAuth2 login failed", ex);

			// 프론트가 없으므로 JSON으로 반환 // 프론트 연동시 제거
			res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			res.setContentType("application/json;charset=UTF-8");
			String message = ex.getMessage() == null ? "oauth2_failed" : ex.getMessage();
			res.getWriter().write("{\"error\":\"" + message + "\"}");

			// 프론트 연동 시 사용할 리다이렉트 (주석 처리)
			// res.sendRedirect("/login?error=" + (ex.getMessage() == null ? "oauth2_failed"
			// : ex.getMessage()));
		};
	}

	// 401(인증) JSON 응답용
	@Bean
	AuthenticationEntryPoint restAuthEntryPoint() {
		return (req, res, ex) -> {
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