package com.trackify.trackify.service;

import com.trackify.trackify.constants.AppConstants;
import com.trackify.trackify.model.CurrentlyPlayingTrackInfo;
import com.trackify.trackify.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

        CurrentlyPlayingTrackInfo currentTrack = spotifyService.getCurrentlyPlayingTrack(user);

        if (currentTrack == null || !currentTrack.isPlaying()) {
            handleNoTrackPlaying(user);
            return;
        }

        boolean trackChanged = hasTrackChanged(user, currentTrack);

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
        } else {
            log.debug("Same track still playing for user {}, refreshing status expiration", user.getSlackUserId());
        }

        // ALWAYS update Slack status to refresh expiration time
        // This is critical: even when track hasn't changed, we need to update the expiration
        // based on current progress to prevent status from expiring mid-song
        slackService.updateUserStatus(
                user,
                currentTrack.getTrackName(),
                currentTrack.getArtistName(),
                currentTrack.getDurationMs(),
                currentTrack.getProgressMs()
        );
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

    public void manualSync(String userId) {
        log.info("Manual sync requested for user {}", userId);

        User user = userService.findBySlackUserId(userId)
                .orElseThrow(() -> new RuntimeException(AppConstants.ERROR_USER_NOT_FOUND));

        syncUserMusicStatus(user);
    }
}
