package com.slackify.slackify.service;

import com.slackify.slackify.model.User;
import com.slackify.slackify.service.SpotifyService.CurrentlyPlayingTrackInfo;
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

    @Scheduled(fixedDelayString = "${slackify.sync.polling-interval}")
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
        // Check if user has Spotify connected
        if (user.getEncryptedSpotifyAccessToken() == null) {
            log.debug("User {} has no Spotify token, skipping sync", user.getSlackUserId());
            return;
        }

        // Get currently playing track from Spotify
        CurrentlyPlayingTrackInfo currentTrack = spotifyService.getCurrentlyPlayingTrack(user);

        if (currentTrack == null || !currentTrack.isPlaying()) {
            // No track playing or playback paused
            handleNoTrackPlaying(user);
            return;
        }

        // Check if the track has changed
        boolean trackChanged = hasTrackChanged(user, currentTrack);

        if (trackChanged) {
            log.info("Track changed for user {}: {} - {}",
                    user.getSlackUserId(),
                    currentTrack.getTrackName(),
                    currentTrack.getArtistName());

            // Update Slack status
            slackService.updateUserStatus(
                    user,
                    currentTrack.getTrackName(),
                    currentTrack.getArtistName()
            );

            // Update user's currently playing info in database
            userService.updateCurrentlyPlaying(
                    user.getId(),
                    currentTrack.getTrackId(),
                    currentTrack.getTrackName(),
                    currentTrack.getArtistName()
            );
        } else {
            log.debug("No track change for user {}, skipping status update", user.getSlackUserId());
        }
    }

    private void handleNoTrackPlaying(User user) {
        // If there was a previously playing track, clear the status
        if (user.getCurrentlyPlayingSongId() != null) {
            log.info("No track playing for user {}, clearing status", user.getSlackUserId());
            slackService.clearUserStatus(user);
            userService.clearCurrentlyPlaying(user.getId());
        }
    }

    private boolean hasTrackChanged(User user, CurrentlyPlayingTrackInfo currentTrack) {
        String previousTrackId = user.getCurrentlyPlayingSongId();

        // If no previous track, this is a new track
        if (previousTrackId == null) {
            return true;
        }

        // Compare track IDs
        return !previousTrackId.equals(currentTrack.getTrackId());
    }

    public void manualSync(String userId) {
        log.info("Manual sync requested for user {}", userId);

        User user = userService.findBySlackUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        syncUserMusicStatus(user);
    }
}
