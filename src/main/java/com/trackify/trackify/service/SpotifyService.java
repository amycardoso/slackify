package com.trackify.trackify.service;

import com.trackify.trackify.config.SpotifyConfig;
import com.trackify.trackify.exception.*;
import com.trackify.trackify.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.core5.http.ParseException;
import org.springframework.stereotype.Service;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.model_objects.miscellaneous.CurrentlyPlaying;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRefreshRequest;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRequest;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;
import se.michaelthelin.spotify.requests.data.player.GetUsersCurrentlyPlayingTrackRequest;
import se.michaelthelin.spotify.requests.data.player.PauseUsersPlaybackRequest;
import se.michaelthelin.spotify.requests.data.player.StartResumeUsersPlaybackRequest;

import java.io.IOException;
import java.net.URI;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpotifyService {

    private final SpotifyConfig spotifyConfig;
    private final UserService userService;

    public URI getAuthorizationUri() {
        SpotifyApi spotifyApi = getSpotifyApi(null);

        AuthorizationCodeUriRequest authorizationCodeUriRequest = spotifyApi.authorizationCodeUri()
                .scope("user-read-currently-playing,user-read-playback-state,user-modify-playback-state")
                .show_dialog(true)
                .build();

        return authorizationCodeUriRequest.execute();
    }

    public AuthorizationCodeCredentials getAccessToken(String code) throws IOException, ParseException, SpotifyWebApiException {
        SpotifyApi spotifyApi = getSpotifyApi(null);

        AuthorizationCodeRequest authorizationCodeRequest = spotifyApi.authorizationCode(code).build();
        return authorizationCodeRequest.execute();
    }

    public AuthorizationCodeCredentials refreshAccessToken(String refreshToken) throws IOException, ParseException, SpotifyWebApiException {
        SpotifyApi spotifyApi = SpotifyApi.builder()
                .setClientId(spotifyConfig.getClientId())
                .setClientSecret(spotifyConfig.getClientSecret())
                .setRefreshToken(refreshToken)
                .build();

        AuthorizationCodeRefreshRequest refreshRequest = spotifyApi.authorizationCodeRefresh().build();
        return refreshRequest.execute();
    }

    public CurrentlyPlayingTrackInfo getCurrentlyPlayingTrack(User user) {
        try {
            // Check if token needs refresh
            if (userService.isSpotifyTokenExpired(user)) {
                log.debug("Spotify token expired for user {}, refreshing...", user.getId());
                refreshUserToken(user);
            }

            String accessToken = userService.getDecryptedSpotifyAccessToken(user);
            SpotifyApi spotifyApi = getSpotifyApi(accessToken);

            GetUsersCurrentlyPlayingTrackRequest request = spotifyApi.getUsersCurrentlyPlayingTrack().build();
            CurrentlyPlaying currentlyPlaying = request.execute();

            if (currentlyPlaying == null || currentlyPlaying.getItem() == null) {
                log.debug("No track currently playing for user {}", user.getId());
                return null;
            }

            if (currentlyPlaying.getItem() instanceof Track) {
                Track track = (Track) currentlyPlaying.getItem();
                String artistName = track.getArtists().length > 0 ? track.getArtists()[0].getName() : "Unknown Artist";

                return CurrentlyPlayingTrackInfo.builder()
                        .trackId(track.getId())
                        .trackName(track.getName())
                        .artistName(artistName)
                        .isPlaying(currentlyPlaying.getIs_playing())
                        .build();
            }

            return null;
        } catch (Exception e) {
            log.error("Error fetching currently playing track for user {}", user.getId(), e);
            return null;
        }
    }

    public void pausePlayback(User user) {
        try {
            String accessToken = userService.getDecryptedSpotifyAccessToken(user);
            SpotifyApi spotifyApi = getSpotifyApi(accessToken);

            PauseUsersPlaybackRequest request = spotifyApi.pauseUsersPlayback().build();
            request.execute();
            log.info("Paused playback for user {}", user.getSlackUserId());
        } catch (SpotifyWebApiException e) {
            handleSpotifyApiException(e, "pause playback");
        } catch (IOException | ParseException e) {
            log.error("Network error pausing playback for user {}", user.getSlackUserId(), e);
            throw new SpotifyException("Network error communicating with Spotify", e);
        }
    }

    public void resumePlayback(User user) {
        try {
            String accessToken = userService.getDecryptedSpotifyAccessToken(user);
            SpotifyApi spotifyApi = getSpotifyApi(accessToken);

            StartResumeUsersPlaybackRequest request = spotifyApi.startResumeUsersPlayback().build();
            request.execute();
            log.info("Resumed playback for user {}", user.getSlackUserId());
        } catch (SpotifyWebApiException e) {
            handleSpotifyApiException(e, "resume playback");
        } catch (IOException | ParseException e) {
            log.error("Network error resuming playback for user {}", user.getSlackUserId(), e);
            throw new SpotifyException("Network error communicating with Spotify", e);
        }
    }

    private void refreshUserToken(User user) throws IOException, ParseException, SpotifyWebApiException {
        String refreshToken = userService.getDecryptedSpotifyRefreshToken(user);
        AuthorizationCodeCredentials credentials = refreshAccessToken(refreshToken);

        userService.updateSpotifyTokens(
                user.getId(),
                user.getSpotifyUserId(),
                credentials.getAccessToken(),
                credentials.getRefreshToken() != null ? credentials.getRefreshToken() : refreshToken,
                credentials.getExpiresIn()
        );
    }

    private void handleSpotifyApiException(SpotifyWebApiException e, String operation) {
        String message = e.getMessage();

        log.error("Spotify API error during {}: message={}", operation, message);

        // Parse error message for specific error types
        if (message != null) {
            String lowerMessage = message.toLowerCase();

            // No active device found
            if (lowerMessage.contains("no active device") || lowerMessage.contains("device not found") ||
                lowerMessage.contains("player command failed")) {
                throw new NoActiveDeviceException();
            }

            // Token expired / Unauthorized
            if (lowerMessage.contains("unauthorized") || lowerMessage.contains("token") ||
                lowerMessage.contains("401")) {
                throw new SpotifyTokenExpiredException();
            }

            // Premium required
            if (lowerMessage.contains("premium") || lowerMessage.contains("403")) {
                throw new SpotifyPremiumRequiredException();
            }

            // Rate limited
            if (lowerMessage.contains("rate limit") || lowerMessage.contains("429") ||
                lowerMessage.contains("too many requests")) {
                throw new SpotifyRateLimitException();
            }
        }

        // Default: Generic Spotify error
        throw new SpotifyException("Spotify API error: " + message, e);
    }

    private SpotifyApi getSpotifyApi(String accessToken) {
        SpotifyApi.Builder builder = SpotifyApi.builder()
                .setClientId(spotifyConfig.getClientId())
                .setClientSecret(spotifyConfig.getClientSecret())
                .setRedirectUri(URI.create(spotifyConfig.getRedirectUri()));

        if (accessToken != null) {
            builder.setAccessToken(accessToken);
        }

        return builder.build();
    }

    @lombok.Data
    @lombok.Builder
    public static class CurrentlyPlayingTrackInfo {
        private String trackId;
        private String trackName;
        private String artistName;
        private boolean isPlaying;
    }
}
