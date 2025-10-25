package com.trackify.trackify.controller;

import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.request.oauth.OAuthV2AccessRequest;
import com.slack.api.methods.response.oauth.OAuthV2AccessResponse;
import com.trackify.trackify.config.SpotifyConfig;
import com.trackify.trackify.model.User;
import com.trackify.trackify.service.SpotifyService;
import com.trackify.trackify.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;

import java.net.URI;

@Slf4j
@Controller
@RequestMapping("/oauth")
@RequiredArgsConstructor
public class OAuthController {

    private final UserService userService;
    private final SpotifyService spotifyService;
    private final SpotifyConfig spotifyConfig;

    @Value("${slack.client-id}")
    private String slackClientId;

    @Value("${slack.client-secret}")
    private String slackClientSecret;

    @Value("${slack.redirect-uri}")
    private String slackRedirectUri;

    private final Slack slack = Slack.getInstance();

    @GetMapping("/slack")
    public RedirectView initiateSlackOAuth() {
        String slackAuthUrl = "https://slack.com/oauth/v2/authorize"
                + "?client_id=" + slackClientId
                + "&scope=users.profile:write,users.profile:read"
                + "&redirect_uri=" + slackRedirectUri;

        log.info("Initiating Slack OAuth flow");
        return new RedirectView(slackAuthUrl);
    }

    @GetMapping("/slack/callback")
    public String handleSlackCallback(@RequestParam("code") String code,
                                      @RequestParam(value = "state", required = false) String state) {
        try {
            log.info("Received Slack OAuth callback with code: {}", code.substring(0, 10) + "...");

            MethodsClient client = slack.methods();
            OAuthV2AccessRequest request = OAuthV2AccessRequest.builder()
                    .clientId(slackClientId)
                    .clientSecret(slackClientSecret)
                    .code(code)
                    .redirectUri(slackRedirectUri)
                    .build();

            OAuthV2AccessResponse response = client.oauthV2Access(request);

            if (!response.isOk()) {
                log.error("Slack OAuth error: {}", response.getError());
                return "redirect:/error?message=slack_auth_failed";
            }

            String userId = response.getAuthedUser().getId();
            String teamId = response.getTeam().getId();
            String accessToken = response.getAuthedUser().getAccessToken();

            // Create or update user
            User user = userService.createOrUpdateUser(userId, teamId, accessToken);

            log.info("Successfully authenticated Slack user: {}", userId);

            // Redirect to Spotify authorization
            return "redirect:/oauth/spotify?userId=" + user.getId();

        } catch (Exception e) {
            log.error("Error handling Slack OAuth callback", e);
            return "redirect:/error?message=slack_auth_error";
        }
    }

    @GetMapping("/spotify")
    public RedirectView initiateSpotifyOAuth(@RequestParam("userId") String userId) {
        log.info("Initiating Spotify OAuth flow for user: {}", userId);

        URI authUri = spotifyService.getAuthorizationUri();

        // In a real implementation, you'd want to store the userId in session or state parameter
        // For simplicity, we're using a query parameter (not recommended for production)
        String redirectUrl = authUri.toString() + "&state=" + userId;

        return new RedirectView(redirectUrl);
    }

    @GetMapping("/spotify/callback")
    public String handleSpotifyCallback(@RequestParam("code") String code,
                                        @RequestParam(value = "state", required = false) String userId,
                                        @RequestParam(value = "error", required = false) String error) {
        try {
            if (error != null) {
                log.error("Spotify OAuth error: {}", error);
                return "redirect:/error?message=spotify_auth_denied";
            }

            log.info("Received Spotify OAuth callback for user: {}", userId);

            // Exchange code for access token
            AuthorizationCodeCredentials credentials = spotifyService.getAccessToken(code);

            // Get Spotify user ID (in a real implementation, fetch from Spotify API)
            String spotifyUserId = "spotify_user_" + System.currentTimeMillis();

            // Update user with Spotify tokens
            userService.updateSpotifyTokens(
                    userId,
                    spotifyUserId,
                    credentials.getAccessToken(),
                    credentials.getRefreshToken(),
                    credentials.getExpiresIn()
            );

            log.info("Successfully authenticated Spotify for user: {}", userId);

            return "redirect:/success";

        } catch (Exception e) {
            log.error("Error handling Spotify OAuth callback", e);
            return "redirect:/error?message=spotify_auth_error";
        }
    }
}
