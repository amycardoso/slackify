package com.trackify.trackify.controller;

import com.trackify.trackify.constants.AppConstants;
import com.trackify.trackify.service.SpotifyService;
import com.trackify.trackify.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @GetMapping("/spotify")
    public RedirectView initiateSpotifyOAuth(@RequestParam("userId") String userId) {
        log.info("Initiating Spotify OAuth flow for user: {}", userId);

        URI authUri = spotifyService.getAuthorizationUri();
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

            // Generate temporary Spotify user ID (will be updated on first sync)
            String spotifyUserId = AppConstants.SPOTIFY_USER_ID_PREFIX + System.currentTimeMillis();

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
