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
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlackService {

    private final UserService userService;
    private final com.slack.api.Slack slack = com.slack.api.Slack.getInstance();

    @Retryable(
            maxAttemptsExpression = "${trackify.retry.max-attempts}",
            backoff = @Backoff(delayExpression = "${trackify.retry.backoff-delay}", multiplier = 2)
    )
    public void updateUserStatus(User user, String songTitle, String artist, Integer durationMs) {
        try {
            UserSettings settings = userService.getUserSettings(user.getId())
                    .orElseThrow(() -> new RuntimeException(AppConstants.ERROR_USER_SETTINGS_NOT_FOUND));

            if (!settings.isSyncEnabled()) {
                log.debug("Sync disabled for user {}, skipping status update", user.getSlackUserId());
                return;
            }

            String statusText = buildStatusText(settings, songTitle, artist);
            String statusEmoji = settings.getDefaultEmoji();

            // Calculate status expiration based on song duration
            Long statusExpiration = null;
            if (durationMs != null && durationMs > 0) {
                // Convert duration from milliseconds to seconds and add to current time
                long currentTimeSeconds = System.currentTimeMillis() / 1000;
                long durationSeconds = durationMs / 1000;
                statusExpiration = currentTimeSeconds + durationSeconds;
            }

            setSlackStatus(user.getSlackAccessToken(), statusText, statusEmoji, statusExpiration);

            log.info("Updated Slack status for user {}: {} (expires in {}s)",
                    user.getSlackUserId(), statusText, durationMs != null ? durationMs / 1000 : "N/A");
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
            log.error("Failed to set Slack status: {}", response.getError());
            throw new RuntimeException("Slack API error: " + response.getError());
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
