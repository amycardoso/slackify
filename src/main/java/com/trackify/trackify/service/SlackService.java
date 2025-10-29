package com.trackify.trackify.service;

import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.users.profile.UsersProfileSetRequest;
import com.slack.api.methods.response.users.profile.UsersProfileSetResponse;
import com.slack.api.model.User.Profile;
import com.trackify.trackify.constants.AppConstants;
import com.trackify.trackify.model.User;
import com.trackify.trackify.model.UserSettings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlackService {

    private final UserService userService;
    private final TokenValidationService tokenValidationService;
    private final com.slack.api.Slack slack = com.slack.api.Slack.getInstance();

    @Value("${trackify.sync.expiration-overhead-ms:120000}")
    private long expirationOverheadMs;

    @Retryable(
            maxAttemptsExpression = "${trackify.retry.max-attempts}",
            backoff = @Backoff(delayExpression = "${trackify.retry.backoff-delay}", multiplier = 2)
    )
    public void updateUserStatus(User user, String songTitle, String artist, Integer durationMs, Integer progressMs) {
        try {
            UserSettings settings = userService.getUserSettings(user.getId())
                    .orElseThrow(() -> new RuntimeException(AppConstants.ERROR_USER_SETTINGS_NOT_FOUND));

            if (!settings.isSyncEnabled()) {
                log.debug("Sync disabled for user {}, skipping status update", user.getSlackUserId());
                return;
            }

            String statusText = buildStatusText(settings, songTitle, artist);
            String statusEmoji = settings.getDefaultEmoji();

            // Calculate status expiration based on remaining song time + overhead
            Long statusExpiration = null;
            if (durationMs != null && durationMs > 0) {
                // Calculate remaining time: duration - current progress + overhead buffer
                int currentProgress = (progressMs != null && progressMs > 0) ? progressMs : 0;
                long remainingMs = durationMs - currentProgress + expirationOverheadMs;

                // Convert to Unix timestamp in seconds
                long currentTimeSeconds = System.currentTimeMillis() / 1000;
                long remainingSeconds = remainingMs / 1000;
                statusExpiration = currentTimeSeconds + remainingSeconds;

                log.debug("Status expiration calculated: remaining={}s (duration={}s, progress={}s, overhead={}s)",
                        remainingSeconds, durationMs / 1000, currentProgress / 1000, expirationOverheadMs / 1000);
            }

            setSlackStatus(user.getSlackAccessToken(), statusText, statusEmoji, statusExpiration);

            userService.updateLastSetStatus(user.getId(), statusText);

            log.info("Updated Slack status for user {}: {} (expires in {}s)",
                    user.getSlackUserId(), statusText, statusExpiration != null ? (statusExpiration - System.currentTimeMillis() / 1000) : "N/A");
        } catch (RuntimeException e) {
            // Check if this is a token invalidation error
            if (e.getMessage() != null && e.getMessage().contains("Slack token invalidated")) {
                log.error("Slack token invalidated for user {}: {}", user.getSlackUserId(), e.getMessage());

                // Mark user as invalidated
                tokenValidationService.markUserAsInvalidated(user, e.getMessage());

                // Try to send notification (might fail if token is completely invalid)
                try {
                    String notificationMessage = "⚠️ *Your Slack connection has been revoked*\n\n" +
                            "Trackify can no longer update your Slack status. " +
                            "To resume automatic status updates, please reinstall the app.";
                    sendMessage(user.getSlackAccessToken(), user.getSlackUserId(), notificationMessage);
                    log.info("Sent invalidation notification to user {}", user.getSlackUserId());
                } catch (Exception notifyError) {
                    log.warn("Could not send invalidation notification to user {}: {}",
                            user.getSlackUserId(), notifyError.getMessage());
                }
            } else {
                log.error("Error updating Slack status for user {}", user.getSlackUserId(), e);
                throw new RuntimeException(AppConstants.ERROR_FAILED_TO_UPDATE_SLACK_STATUS, e);
            }
        } catch (Exception e) {
            log.error("Error updating Slack status for user {}", user.getSlackUserId(), e);
            throw new RuntimeException(AppConstants.ERROR_FAILED_TO_UPDATE_SLACK_STATUS, e);
        }
    }

    @Retryable(
            maxAttemptsExpression = "${trackify.retry.max-attempts}",
            backoff = @Backoff(delayExpression = "${trackify.retry.backoff-delay}", multiplier = 2)
    )
    public void clearUserStatus(User user) {
        try {
            setSlackStatus(user.getSlackAccessToken(), "", "", null);
            log.info("Cleared Slack status for user {}", user.getSlackUserId());
        } catch (Exception e) {
            log.error("Error clearing Slack status for user {}", user.getSlackUserId(), e);
            throw new RuntimeException(AppConstants.ERROR_FAILED_TO_CLEAR_SLACK_STATUS, e);
        }
    }

    private void setSlackStatus(String accessToken, String statusText, String statusEmoji, Long statusExpiration)
            throws IOException, SlackApiException {
        MethodsClient client = slack.methods(accessToken);

        Profile profile = new Profile();
        profile.setStatusText(statusText);
        profile.setStatusEmoji(statusEmoji);

        // Set status expiration if provided
        if (statusExpiration != null) {
            profile.setStatusExpiration(statusExpiration);
        }

        UsersProfileSetRequest request = UsersProfileSetRequest.builder()
                .profile(profile)
                .build();

        UsersProfileSetResponse response = client.usersProfileSet(request);

        if (!response.isOk()) {
            String error = response.getError();
            log.error("Failed to set Slack status: {}", error);

            // Check if error indicates token invalidation
            if (tokenValidationService.isSlackTokenInvalidError(error)) {
                throw new RuntimeException("Slack token invalidated: " + error);
            }

            throw new RuntimeException("Slack API error: " + error);
        }
    }

    private String buildStatusText(UserSettings settings, String songTitle, String artist) {
        String template = settings.getStatusTemplate();

        // Replace placeholders in template (emoji is set separately via statusEmoji field)
        String text = template
                .replace(AppConstants.PLACEHOLDER_EMOJI, "")
                .replace(AppConstants.PLACEHOLDER_TITLE, settings.isShowSongTitle() ? songTitle : "")
                .replace(AppConstants.PLACEHOLDER_ARTIST, settings.isShowArtist() ? artist : "");

        // Clean up extra spaces and dashes
        text = text.replaceAll("\\s+-\\s+$", "")
                   .replaceAll("^\\s+-\\s+", "")
                   .replaceAll("\\s{2,}", " ")
                   .trim();

        return text;
    }

    /**
     * Fetches the user's timezone offset from Slack API.
     * Returns timezone offset in seconds from UTC, or null if unable to fetch.
     */
    public Integer getUserTimezoneOffset(String accessToken, String userId) {
        try {
            MethodsClient client = slack.methods(accessToken);
            var response = client.usersInfo(req -> req.user(userId));

            if (response.isOk() && response.getUser() != null) {
                Integer tzOffset = response.getUser().getTzOffset();
                log.debug("Fetched timezone offset for user {}: {} seconds", userId, tzOffset);
                return tzOffset;
            } else {
                log.warn("Failed to fetch user info for {}: {}", userId, response.getError());
                return null;
            }
        } catch (Exception e) {
            log.error("Error fetching user info for {}", userId, e);
            return null;
        }
    }

    /**
     * Fetches the current Slack status for a user.
     * Returns null if unable to fetch (e.g., network error, invalid token).
     */
    public String getCurrentStatusText(User user) {
        try {
            MethodsClient client = slack.methods(user.getSlackAccessToken());
            var response = client.usersProfileGet(req -> req.user(user.getSlackUserId()));

            if (response.isOk() && response.getProfile() != null) {
                String statusText = response.getProfile().getStatusText();
                log.debug("Fetched current status for user {}: {}", user.getSlackUserId(), statusText);
                return statusText != null ? statusText : "";
            } else {
                log.warn("Failed to fetch current status for user {}: {}", user.getSlackUserId(), response.getError());
                return null;
            }
        } catch (Exception e) {
            log.error("Error fetching current status for user {}", user.getSlackUserId(), e);
            return null;
        }
    }

    /**
     * Checks if the user has manually changed their Slack status.
     * Returns true if the current status differs from what we last set.
     */
    public boolean hasManualStatusChange(User user) {
        String currentStatus = getCurrentStatusText(user);

        if (currentStatus == null) {
            log.debug("Could not fetch current status for user {}, assuming no manual change", user.getSlackUserId());
            return false;
        }

        String lastSetStatus = user.getLastSetStatusText();

        if (lastSetStatus == null) {
            boolean isManual = !currentStatus.isEmpty();
            log.debug("No previous status set for user {}, current='{}', manual={}",
                    user.getSlackUserId(), currentStatus, isManual);
            return isManual;
        }

        boolean hasChanged = !normalizeStatusText(currentStatus).equals(normalizeStatusText(lastSetStatus));

        if (hasChanged) {
            log.info("Manual status change detected for user {}: '{}' -> '{}'",
                    user.getSlackUserId(), lastSetStatus, currentStatus);
        }

        return hasChanged;
    }

    /**
     * Normalizes status text for comparison by trimming and handling HTML entities.
     */
    private String normalizeStatusText(String statusText) {
        if (statusText == null) {
            return "";
        }
        return statusText.trim()
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'");
    }

    public String sendMessage(String accessToken, String channel, String message) {
        try {
            MethodsClient client = slack.methods(accessToken);
            var response = client.chatPostMessage(req -> req
                    .channel(channel)
                    .text(message)
            );

            if (!response.isOk()) {
                log.error("Failed to send Slack message: {}", response.getError());
                throw new RuntimeException("Slack API error: " + response.getError());
            }

            return response.getTs();
        } catch (Exception e) {
            log.error("Error sending Slack message", e);
            throw new RuntimeException(AppConstants.ERROR_FAILED_TO_SEND_SLACK_MESSAGE, e);
        }
    }
}
