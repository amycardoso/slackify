package com.trackify.trackify.config;

import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.trackify.trackify.service.MongoDBInstallationService;
import com.trackify.trackify.service.MongoDBOAuthStateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
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
    public App slackApp(
            AppConfig appConfig,
            MongoDBInstallationService installationService,
            MongoDBOAuthStateService oauthStateService) {

        App app = new App(appConfig);

        // Configure OAuth settings with custom services
        app.asOAuthApp(true);
        app.service(installationService);
        app.service(oauthStateService);

        // Add OAuth completion endpoints
        app.endpoint("GET", "/slack/oauth/completion", (req, ctx) -> {
            return com.slack.api.bolt.response.Response.builder()
                    .statusCode(200)
                    .contentType("text/html")
                    .body("<html><body><h1>Installation Successful!</h1>" +
                          "<p>Trackify has been installed to your workspace.</p>" +
                          "<p>Now, please authorize Spotify: <a href='/oauth/spotify'>Connect Spotify</a></p>" +
                          "</body></html>")
                    .build();
        });

        app.endpoint("GET", "/slack/oauth/cancellation", (req, ctx) -> {
            return com.slack.api.bolt.response.Response.builder()
                    .statusCode(200)
                    .contentType("text/html")
                    .body("<html><body><h1>Installation Cancelled</h1>" +
                          "<p>The Slack app installation was cancelled.</p>" +
                          "</body></html>")
                    .build();
        });

        log.info("Slack App bean created with config - clientId: {}, signingSecret present: {}",
                clientId != null && !clientId.isEmpty() ? "present" : "missing",
                signingSecret != null && !signingSecret.isEmpty());
        log.info("Using MongoDB-based InstallationService and OAuthStateService");
        log.info("Bolt OAuth endpoints configured: /slack/install, /slack/oauth_redirect");

        return app;
    }
}

