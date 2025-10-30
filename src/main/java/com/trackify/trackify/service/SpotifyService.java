package com.trackify.trackify.service;

import com.trackify.trackify.config.SpotifyConfig;
import com.trackify.trackify.constants.AppConstants;
import com.trackify.trackify.exception.*;
import com.trackify.trackify.model.CurrentlyPlayingTrackInfo;
import com.trackify.trackify.model.SpotifyDevice;
import com.trackify.trackify.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.core5.http.ParseException;
import org.springframework.stereotype.Service;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.exceptions.detailed.ForbiddenException;
import se.michaelthelin.spotify.exceptions.detailed.NotFoundException;
import se.michaelthelin.spotify.exceptions.detailed.TooManyRequestsException;
import se.michaelthelin.spotify.exceptions.detailed.UnauthorizedException;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.model_objects.miscellaneous.CurrentlyPlaying;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRefreshRequest;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRequest;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpotifyService {

    private final SpotifyConfig spotifyConfig;
    private final UserService userService;
    private final TokenValidationService tokenValidationService;

    public URI getAuthorizationUri() {
        SpotifyApi spotifyApi = getSpotifyApi(null);

        AuthorizationCodeUriRequest authorizationCodeUriRequest = spotifyApi.authorizationCodeUri()
                .scope(spotifyConfig.getScope())
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

    /**
     * Gets the list of available Spotify devices for a user.
     */
    public List<SpotifyDevice> getAvailableDevices(User user) {
        try {
            // Check if token needs refresh
            if (userService.isSpotifyTokenExpired(user)) {
                log.debug("Spotify token expired for user {}, refreshing...", user.getId());
                refreshUserToken(user);
            }

            String accessToken = userService.getDecryptedSpotifyAccessToken(user);
            SpotifyApi spotifyApi = getSpotifyApi(accessToken);

            var request = spotifyApi.getUsersAvailableDevices().build();
            var devices = request.execute();

            if (devices == null || devices.length == 0) {
                log.debug("No devices found for user {}", user.getId());
                return new ArrayList<>();
            }

            return Arrays.stream(devices)
                    .map(device -> SpotifyDevice.builder()
                            .id(device.getId())
                            .name(device.getName())
                            .type(device.getType())
                            .isActive(device.getIs_active())
                            .build())
                    .collect(Collectors.toList());

        } catch (UnauthorizedException e) {
            log.warn("Unauthorized error for user {}: {}", user.getSlackUserId(), e.getMessage());
            handleSpotifyTokenError(user, e.getMessage());
            return new ArrayList<>();
        } catch (SpotifyWebApiException e) {
            String errorMsg = e.getMessage();
            if (tokenValidationService.isSpotifyTokenInvalidError(errorMsg)) {
                log.warn("Token invalidation detected for user {}: {}", user.getSlackUserId(), errorMsg);
                handleSpotifyTokenError(user, errorMsg);
            } else {
                log.error("Spotify API error for user {}: {}", user.getSlackUserId(), errorMsg);
            }
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("Error fetching devices for user {}", user.getId(), e);
            return new ArrayList<>();
        }
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

            var request = spotifyApi.getUsersCurrentlyPlayingTrack().build();
            CurrentlyPlaying currentlyPlaying = request.execute();

            if (currentlyPlaying == null || currentlyPlaying.getItem() == null) {
                log.debug("No track currently playing for user {}", user.getId());
                return null;
            }

            if (currentlyPlaying.getItem() instanceof Track) {
                Track track = (Track) currentlyPlaying.getItem();
                String artistName = track.getArtists().length > 0 ? track.getArtists()[0].getName() : AppConstants.UNKNOWN_ARTIST;

                // Get device information
                String deviceId = currentlyPlaying.getDevice() != null ? currentlyPlaying.getDevice().getId() : null;
                String deviceName = currentlyPlaying.getDevice() != null ? currentlyPlaying.getDevice().getName() : null;

                return CurrentlyPlayingTrackInfo.builder()
                        .trackId(track.getId())
                        .trackName(track.getName())
                        .artistName(artistName)
                        .isPlaying(currentlyPlaying.getIs_playing())
                        .durationMs(track.getDurationMs())
                        .progressMs(currentlyPlaying.getProgress_ms())
                        .deviceId(deviceId)
                        .deviceName(deviceName)
                        .build();
            }

            return null;
        } catch (UnauthorizedException e) {
            // Token is invalid or expired
            log.warn("Unauthorized error for user {}: {}", user.getSlackUserId(), e.getMessage());
            handleSpotifyTokenError(user, e.getMessage());
            return null;
        } catch (SpotifyWebApiException e) {
            // Check if error message indicates token invalidation
            String errorMsg = e.getMessage();
            if (tokenValidationService.isSpotifyTokenInvalidError(errorMsg)) {
                log.warn("Token invalidation detected for user {}: {}", user.getSlackUserId(), errorMsg);
                handleSpotifyTokenError(user, errorMsg);
            } else {
                log.error("Spotify API error for user {}: {}", user.getSlackUserId(), errorMsg);
            }
            return null;
        } catch (Exception e) {
            log.error("Error fetching currently playing track for user {}", user.getId(), e);
            return null;
        }
    }

    /**
     * Handles Spotify token errors by checking if token should be invalidated.
     */
    private void handleSpotifyTokenError(User user, String errorMessage) {
        // Mark user as invalidated in database
        tokenValidationService.markUserAsInvalidated(user, errorMessage);

        // Notification will be handled by App Home or separate notification service in the future
        log.warn("Spotify token invalidated for user {}. User should be notified to reconnect.",
                user.getSlackUserId());
    }

    public void pausePlayback(User user) {
        executePlayerCommand(user, "pause", () -> {
            String accessToken = userService.getDecryptedSpotifyAccessToken(user);
            SpotifyApi spotifyApi = getSpotifyApi(accessToken);
            spotifyApi.pauseUsersPlayback().build().execute();
        });
    }

    public void resumePlayback(User user) {
        executePlayerCommand(user, "resume", () -> {
            String accessToken = userService.getDecryptedSpotifyAccessToken(user);
            SpotifyApi spotifyApi = getSpotifyApi(accessToken);
            spotifyApi.startResumeUsersPlayback().build().execute();
        });
    }

    private void executePlayerCommand(User user, String operation, PlayerCommand command) {
        try {
            command.execute();
            log.info("{}d playback for user {}", operation, user.getSlackUserId());
        } catch (NotFoundException e) {
            log.warn("No active device found for user {}", user.getSlackUserId());
            throw new NoActiveDeviceException();
        } catch (UnauthorizedException e) {
            log.warn("Unauthorized - token expired for user {}", user.getSlackUserId());
            throw new SpotifyTokenExpiredException();
        } catch (ForbiddenException e) {
            log.warn("Forbidden - Premium required for user {}", user.getSlackUserId());
            throw new SpotifyPremiumRequiredException();
        } catch (TooManyRequestsException e) {
            log.warn("Rate limited for user {}", user.getSlackUserId());
            throw new SpotifyRateLimitException();
        } catch (SpotifyWebApiException e) {
            log.error("Spotify API error {}ing playback for user {}: {}", operation, user.getSlackUserId(), e.getMessage(), e);
            throw new SpotifyException("Spotify API error: " + e.getMessage(), e);
        } catch (IOException | ParseException e) {
            log.error("Network error {}ing playback for user {}", operation, user.getSlackUserId(), e);
            throw new SpotifyException("Network error communicating with Spotify", e);
        }
    }

    @FunctionalInterface
    private interface PlayerCommand {
        void execute() throws IOException, ParseException, SpotifyWebApiException;
    }

    private void refreshUserToken(User user) throws IOException, ParseException, SpotifyWebApiException {
        try {
            String refreshToken = userService.getDecryptedSpotifyRefreshToken(user);
            AuthorizationCodeCredentials credentials = refreshAccessToken(refreshToken);

            userService.updateSpotifyTokens(
                    user.getId(),
                    user.getSpotifyUserId(),
                    credentials.getAccessToken(),
                    credentials.getRefreshToken() != null ? credentials.getRefreshToken() : refreshToken,
                    credentials.getExpiresIn()
            );
        } catch (SpotifyWebApiException e) {
            // Check if this is an "invalid_grant" error (token revoked)
            String errorMsg = e.getMessage();
            if (tokenValidationService.isSpotifyTokenInvalidError(errorMsg)) {
                log.error("Token refresh failed - token has been revoked for user {}: {}",
                        user.getSlackUserId(), errorMsg);
                handleSpotifyTokenError(user, errorMsg);
                throw e; // Re-throw to stop sync
            } else {
                // Other error, re-throw
                throw e;
            }
        }
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
}
