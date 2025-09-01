package com.dgu.review.global.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        Info info = new Info()
                .title("Re-View Server API")
                .description("ReView API 명세서")
                .version("1.0.0");

        return new OpenAPI()
                .addServersItem(new Server().url("/"))
                .info(info);
    }

}
