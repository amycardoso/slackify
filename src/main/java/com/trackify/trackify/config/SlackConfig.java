package com.trackify.trackify.config;

import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.bolt.service.builtin.oauth.OAuthErrorHandler;
import com.slack.api.bolt.service.builtin.oauth.OAuthV2SuccessHandler;
import com.slack.api.bolt.request.builtin.OAuthCallbackRequest;
import com.slack.api.methods.response.oauth.OAuthV2AccessResponse;
import com.trackify.trackify.service.MongoDBInstallationService;
import com.trackify.trackify.service.MongoDBOAuthStateService;
import com.trackify.trackify.service.OAuthTemplateService;
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
                .scope("commands")
                .userScope("users.profile:read,users.profile:write")
                .oauthInstallPath("/slack/install")
                .oauthRedirectUriPath("/slack/oauth_redirect")
                .build();
    }

    @Bean
    public App slackApp(
            AppConfig appConfig,
            MongoDBInstallationService installationService,
            MongoDBOAuthStateService oauthStateService,
            OAuthTemplateService templateService) {

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
                String errorHtml = templateService.renderError(
                        "Installation Error",
                        "Something went wrong during the installation process.",
                        "User account not found after OAuth completion."
                );
                return com.slack.api.bolt.response.Response.builder()
                        .statusCode(500)
                        .contentType("text/html")
                        .body(errorHtml)
                        .build();
            }

            // Generate Spotify OAuth link with userId
            String spotifyAuthLink = "/oauth/spotify?userId=" + userId;
            String successHtml = templateService.renderSuccess(spotifyAuthLink);

            return com.slack.api.bolt.response.Response.builder()
                    .statusCode(200)
                    .contentType("text/html")
                    .body(successHtml)
                    .build();
        });

        // OAuth Error Handler - called if OAuth fails
        app.oauthCallbackError((OAuthErrorHandler) (req, resp) -> {
            String error = req.getPayload().getError();
            log.error("Slack OAuth error: {}", error);

            String errorHtml = templateService.renderError(
                    "Installation Failed",
                    "The Slack app installation could not be completed.",
                    "Error: " + (error != null ? error : "Unknown error")
            );

            return com.slack.api.bolt.response.Response.builder()
                    .statusCode(200)
                    .contentType("text/html")
                    .body(errorHtml)
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

