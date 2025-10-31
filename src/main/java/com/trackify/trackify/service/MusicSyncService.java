package com.trackify.trackify.service;

import com.trackify.trackify.constants.AppConstants;
import com.trackify.trackify.model.CurrentlyPlayingTrackInfo;
import com.trackify.trackify.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MusicSyncService {

    private final UserService userService;
    private final SpotifyService spotifyService;
    private final SlackService slackService;
    private final TimezoneService timezoneService;

    @Value("${trackify.sync.polling-interval:10000}")
    private long pollingIntervalMs;

    @Value("${trackify.sync.expiration-overhead-ms:120000}")
    private long expirationOverheadMs;

    @Scheduled(fixedDelayString = "${trackify.sync.polling-interval}")
    public void syncMusicStatus() {
        log.debug("Starting music status sync cycle");

        List<User> activeUsers = userService.findAllActiveUsers();
        log.debug("Found {} active users to sync", activeUsers.size());

        for (User user : activeUsers) {
            try {
                syncUserMusicStatus(user);
            } catch (Exception e) {
                log.error("Error syncing music status for user {}", user.getSlackUserId(), e);
                // Continue with next user even if this one fails
            }
        }

        log.debug("Completed music status sync cycle");
    }

    private void syncUserMusicStatus(User user) {
        if (user.getEncryptedSpotifyAccessToken() == null) {
            log.debug("User {} has no Spotify token, skipping sync", user.getSlackUserId());
            return;
        }

        // Check if user has token invalidated
        if (user.isTokenInvalidated()) {
            log.debug("User {} has invalidated token, skipping sync", user.getSlackUserId());
            return;
        }

        // Check working hours if enabled
        if (!isWithinWorkingHours(user)) {
            log.debug("User {} is outside working hours, skipping sync", user.getSlackUserId());
            return;
        }

        CurrentlyPlayingTrackInfo currentTrack = spotifyService.getCurrentlyPlayingTrack(user);

        if (currentTrack == null || !currentTrack.isPlaying()) {
            handleNoTrackPlaying(user);
            return;
        }

        // Check if device is allowed
        if (!isDeviceAllowed(user, currentTrack.getDeviceId())) {
            log.debug("User {} is playing on non-tracked device '{}', skipping sync",
                    user.getSlackUserId(), currentTrack.getDeviceName());
            handleNoTrackPlaying(user); // Clear status if device is not allowed
            return;
        }

        boolean trackChanged = hasTrackChanged(user, currentTrack);
        boolean needsExpirationRefresh = shouldRefreshExpiration(currentTrack);
        boolean shouldUpdateStatus = trackChanged || needsExpirationRefresh;

        // Check for manual status changes
        if (!user.isManualStatusSet() && slackService.hasManualStatusChange(user)) {
            log.info("User {} has manually changed their status, pausing automatic updates", user.getSlackUserId());
            userService.setManualStatusFlag(user.getId(), true);
            return; // Don't override manual status
        }

        // If user has manual status set, skip automatic updates
        if (user.isManualStatusSet()) {
            // But clear the flag if track changed - new track = new automatic update
            if (trackChanged) {
                log.info("Track changed for user {} while manual status was set, resuming automatic updates",
                        user.getSlackUserId());
                userService.setManualStatusFlag(user.getId(), false);
            } else {
                log.debug("User {} has manual status set, skipping automatic update", user.getSlackUserId());
                return;
            }
        }

        if (trackChanged) {
            log.info("Track changed for user {}: {} - {}",
                    user.getSlackUserId(),
                    currentTrack.getTrackName(),
                    currentTrack.getArtistName());

            userService.updateCurrentlyPlaying(
                    user.getId(),
                    currentTrack.getTrackId(),
                    currentTrack.getTrackName(),
                    currentTrack.getArtistName()
            );
        }

        if (shouldUpdateStatus) {
            if (needsExpirationRefresh && !trackChanged) {
                log.debug("Same track playing for user {}, but expiration approaching - refreshing status", user.getSlackUserId());
            }

            slackService.updateUserStatus(
                    user,
                    currentTrack.getTrackName(),
                    currentTrack.getArtistName(),
                    currentTrack.getDurationMs(),
                    currentTrack.getProgressMs()
            );
        } else {
            log.debug("Same track playing for user {}, expiration still valid - skipping update", user.getSlackUserId());
        }
    }

    /**
     * Determines if we should refresh the status expiration.
     * Returns true if the remaining time is less than 3x the polling interval.
     * This ensures we refresh before the status expires, accounting for:
     * - 2x polling interval: buffer for timing variations
     * - 1x polling interval: ensures at least one more chance to update
     */
    private boolean shouldRefreshExpiration(CurrentlyPlayingTrackInfo currentTrack) {
        if (currentTrack.getDurationMs() == null || currentTrack.getProgressMs() == null) {
            return true; // If we don't have progress info, update to be safe
        }

        long remainingMs = currentTrack.getDurationMs() - currentTrack.getProgressMs();
        long refreshThresholdMs = (pollingIntervalMs * 3) + expirationOverheadMs;

        boolean shouldRefresh = remainingMs <= refreshThresholdMs;

        if (shouldRefresh) {
            log.debug("Expiration refresh needed: remaining={}s, threshold={}s",
                    remainingMs / 1000, refreshThresholdMs / 1000);
        }

        return shouldRefresh;
    }

    private void handleNoTrackPlaying(User user) {
        if (user.getCurrentlyPlayingSongId() != null) {
            log.info("No track playing for user {}, clearing status", user.getSlackUserId());
            slackService.clearUserStatus(user);
            userService.clearCurrentlyPlaying(user.getId());
        }
    }

    private boolean hasTrackChanged(User user, CurrentlyPlayingTrackInfo currentTrack) {
        String previousTrackId = user.getCurrentlyPlayingSongId();

        if (previousTrackId == null) {
            return true;
        }

        return !previousTrackId.equals(currentTrack.getTrackId());
    }

    /**
     * Checks if the current device is in the list of allowed devices.
     * Returns true if no device filter is configured (all devices allowed) or if device is in the list.
     * Returns false if device filter is configured and device is not in the list.
     */
    private boolean isDeviceAllowed(User user, String deviceId) {
        if (deviceId == null) {
            log.debug("No device ID available for user {}, allowing sync", user.getSlackUserId());
            return true; // If no device info, allow sync
        }

        var settings = userService.getUserSettings(user.getId());

        if (settings.isEmpty()) {
            log.warn("No settings found for user {}, allowing sync", user.getSlackUserId());
            return true; // No settings = allow sync
        }

        var userSettings = settings.get();

        // If no device filter configured, allow all devices
        if (userSettings.getAllowedDeviceIds() == null || userSettings.getAllowedDeviceIds().isEmpty()) {
            return true;
        }

        // Check if current device is in the allowed list
        boolean isAllowed = userSettings.getAllowedDeviceIds().contains(deviceId);

        if (!isAllowed) {
            log.debug("Device {} not in allowed list for user {}", deviceId, user.getSlackUserId());
        }

        return isAllowed;
    }

    /**
     * Checks if the user is within their configured working hours.
     * Returns true if working hours are disabled or if current time is within working hours.
     * Returns false if outside working hours.
     */
    private boolean isWithinWorkingHours(User user) {
        var settings = userService.getUserSettings(user.getId());

        if (settings.isEmpty()) {
            log.warn("No settings found for user {}, allowing sync", user.getSlackUserId());
            return true; // No settings = allow sync
        }

        var userSettings = settings.get();

        // If working hours not enabled, sync 24/7
        if (!userSettings.isWorkingHoursEnabled()) {
            return true;
        }

        // Check if working hours are configured
        if (userSettings.getSyncStartHour() == null || userSettings.getSyncEndHour() == null) {
            log.debug("Working hours enabled but not configured for user {}, allowing sync", user.getSlackUserId());
            return true;
        }

        // Check if current time is within working hours (in UTC)
        boolean isWithin = timezoneService.isWithinWorkingHours(
                userSettings.getSyncStartHour(),
                userSettings.getSyncEndHour()
        );

        if (!isWithin) {
            log.debug("User {} is outside working hours (UTC {} - {}), skipping sync",
                    user.getSlackUserId(),
                    formatHHMM(userSettings.getSyncStartHour()),
                    formatHHMM(userSettings.getSyncEndHour()));
        }

        return isWithin;
    }

    /**
     * Formats HHMM integer as readable time string for logging.
     */
    private String formatHHMM(int hhmm) {
        int hour = hhmm / 100;
        int minute = hhmm % 100;
        return String.format("%02d:%02d", hour, minute);
    }

    public void manualSync(String userId) {
        log.info("Manual sync requested for user {}", userId);

        User user = userService.findBySlackUserId(userId)
                .orElseThrow(() -> new RuntimeException(AppConstants.ERROR_USER_NOT_FOUND));

        syncUserMusicStatus(user);
    }
}
