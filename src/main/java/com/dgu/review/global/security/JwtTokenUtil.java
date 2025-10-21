package com.dgu.review.global.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.dgu.review.global.configuration.JwtConfig;

@Component
public class JwtTokenUtil {

	private String secretKey;
	 private final SecretKey key;
	

    @Autowired
    public JwtTokenUtil(JwtConfig jwtConfig) {
        this.secretKey = jwtConfig.getSecretKey();
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    private static final long EXPIRATION_TIME = 864_000_00; // 1일

    // JWT 생성
    public String generateToken(Long userId,String kakaoId, String username, Map<String, Object> claims) {
    	claims.put("name", username); 
    	claims.put("userId", userId);
    	// SecretKey 객체 생성 (Base64로 안전하게 변환)
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(kakaoId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME)) 
                .signWith(key) 
                .compact();
    }

    // JWT에서 kakaoId 추출
    public String extractKakaoId(String token) {
        return Jwts.parser()
                .setSigningKey(key)
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
    // Jwt에서 claims 추출 
    public String extractClaim(String token, String claimKey) {
        return (String) Jwts.parser()
                .setSigningKey(key)
                .parseClaimsJws(token)
                .getBody()
                .get(claimKey);  
    }
    // 토큰 만료 여부 체크
    public boolean isTokenExpired(String token) {
        return extractExpirationDate(token).before(new Date());
    }

    // 토큰에서 만료 시간 추출
    private Date extractExpirationDate(String token) {
        return Jwts.parser()
                .setSigningKey(key)
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();
    }

    // 토큰 검증
    public boolean validateToken(String token, String kakaoId) {
        return (kakaoId.equals(extractKakaoId(token)) && !isTokenExpired(token));
    }
}
