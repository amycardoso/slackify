package com.trackify.trackify.config;

import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.bolt.service.builtin.oauth.OAuthErrorHandler;
import com.slack.api.bolt.service.builtin.oauth.OAuthV2SuccessHandler;
import com.slack.api.bolt.request.builtin.OAuthCallbackRequest;
import com.slack.api.methods.response.oauth.OAuthV2AccessResponse;
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

        // OAuth V2 Success Handler - called after Slack OAuth completes
        app.oauthCallback((OAuthV2SuccessHandler) (req, resp, oauthAccess) -> {
            String slackUserId = oauthAccess.getAuthedUser().getId();
            String teamId = oauthAccess.getTeam().getId();

            log.info("Slack OAuth completed for user: {} in team: {}", slackUserId, teamId);

            // Find the user we just created in MongoDB
            String userId = installationService.findUserIdBySlackUserId(slackUserId);

            if (userId == null) {
                log.error("Could not find user after OAuth completion for slackUserId: {}", slackUserId);
                return com.slack.api.bolt.response.Response.builder()
                        .statusCode(500)
                        .contentType("text/html")
                        .body("<html><body><h1>Error</h1>" +
                              "<p>Installation completed but user not found. Please try again.</p>" +
                              "</body></html>")
                        .build();
            }

            // Generate Spotify OAuth link with userId
            String spotifyAuthLink = "/oauth/spotify?userId=" + userId;

            return com.slack.api.bolt.response.Response.builder()
                    .statusCode(200)
                    .contentType("text/html")
                    .body("<html><body><h1>Slack Connected!</h1>" +
                          "<p>Trackify has been installed to your workspace.</p>" +
                          "<p><strong>Next step:</strong> Connect your Spotify account to enable music sync.</p>" +
                          "<p><a href='" + spotifyAuthLink + "' style='display: inline-block; padding: 10px 20px; background: #1DB954; color: white; text-decoration: none; border-radius: 5px;'>Connect Spotify</a></p>" +
                          "</body></html>")
                    .build();
        });

        // OAuth Error Handler - called if OAuth fails
        app.oauthCallbackError((OAuthErrorHandler) (req, resp) -> {
            String error = req.getPayload().getError();
            log.error("Slack OAuth error: {}", error);
            return com.slack.api.bolt.response.Response.builder()
                    .statusCode(200)
                    .contentType("text/html")
                    .body("<html><body><h1>Installation Failed</h1>" +
                          "<p>The Slack app installation failed.</p>" +
                          "<p>Error: " + (error != null ? error : "Unknown") + "</p>" +
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

