package com.dgu.review.global.configuration;

import com.google.common.util.concurrent.RateLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

import java.util.concurrent.Executor;

@Configuration
public class BedrockConfig {

    @Bean
    public BedrockRuntimeClient bedrockRuntimeClient() {
        return BedrockRuntimeClient.builder()
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean(name = "llmExecutor")
    public Executor llmExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(1);
        ex.setMaxPoolSize(1);
        ex.setQueueCapacity(100);
        ex.setThreadNamePrefix("llm-");
        ex.initialize();
        return ex;
    }

    @Bean
    public RateLimiter tokenLimiter(
            @Value("${bedrock.limits.tpm:20000}") int tpm
    ) {
        double permitsPerSecond = tpm / 60.0;
        return RateLimiter.create(permitsPerSecond);
    }
}
