package com.trackify.trackify.config;

import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SlackConfig {

    @Value("${slack.client-id}")
    private String clientId;

    @Value("${slack.client-secret}")
    private String clientSecret;

    @Value("${slack.signing-secret}")
    private String signingSecret;

    @Bean
    public AppConfig appConfig() {
        return AppConfig.builder()
                .clientId(clientId)
                .clientSecret(clientSecret)
                .signingSecret(signingSecret)
                .build();
    }

    @Bean
    public App slackApp(AppConfig appConfig) {
        App app = new App(appConfig);

        // Configure OAuth settings
        app.asOAuthApp(true);

        return app;
    }
}
