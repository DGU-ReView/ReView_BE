package com.dgu.review.global.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;

import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;

public class HttpCookieOAuth2AuthorizationRequestRepository
		implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

	private static final String COOKIE_NAME = "oauth2_auth_request";
	private static final int EXPIRE_SECONDS = 180; // 3분
	@Override
	public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
	    return CookieUtils.getCookie(request, COOKIE_NAME)
	            .map(cookie -> CookieUtils.deserialize(cookie, OAuth2AuthorizationRequest.class))
	            .orElse(null);
	}

	@Override
	public void saveAuthorizationRequest(OAuth2AuthorizationRequest request,
	                                     HttpServletRequest request1,
	                                     HttpServletResponse response) {
	    if (request == null) {
	        CookieUtils.deleteCookie(response, COOKIE_NAME);
	    } else {
	        CookieUtils.addCookie(response, COOKIE_NAME,
	                CookieUtils.serialize(request), EXPIRE_SECONDS);
	    }
	}

	@Override
	public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
	                                                             HttpServletResponse response) {
	    OAuth2AuthorizationRequest req = this.loadAuthorizationRequest(request);
	    CookieUtils.deleteCookie(response, COOKIE_NAME);
	    return req;
	}

}
