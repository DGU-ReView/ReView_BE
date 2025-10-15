package com.dgu.review.global.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtConfig {

    @Value("${jwt.secret}")
    private String secretKey;
    
    public String getSecretKey() {
        return secretKey;
    }

}