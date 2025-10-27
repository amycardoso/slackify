package com.trackify.trackify.service;

import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.users.profile.UsersProfileSetRequest;
import com.slack.api.methods.response.users.profile.UsersProfileSetResponse;
import com.slack.api.model.User.Profile;
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
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void updateUserStatus(User user, String songTitle, String artist) {
        try {
            UserSettings settings = userService.getUserSettings(user.getId())
                    .orElseThrow(() -> new RuntimeException("User settings not found"));

            if (!settings.isSyncEnabled()) {
                log.debug("Sync disabled for user {}, skipping status update", user.getSlackUserId());
                return;
            }

            String statusText = buildStatusText(settings, songTitle, artist);
            String statusEmoji = settings.getDefaultEmoji();

            setSlackStatus(user.getSlackAccessToken(), statusText, statusEmoji);

            log.info("Updated Slack status for user {}: {}", user.getSlackUserId(), statusText);

            if (settings.isNotificationsEnabled()) {
                // Optional: Send a DM notification to the user
                // This can be implemented if needed
            }
        } catch (Exception e) {
            log.error("Error updating Slack status for user {}", user.getSlackUserId(), e);
            throw new RuntimeException("Failed to update Slack status", e);
        }
    }

    @Retryable(
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void clearUserStatus(User user) {
        try {
            setSlackStatus(user.getSlackAccessToken(), "", "");
            log.info("Cleared Slack status for user {}", user.getSlackUserId());
        } catch (Exception e) {
            log.error("Error clearing Slack status for user {}", user.getSlackUserId(), e);
            throw new RuntimeException("Failed to clear Slack status", e);
        }
    }

    private void setSlackStatus(String accessToken, String statusText, String statusEmoji)
            throws IOException, SlackApiException {
        MethodsClient client = slack.methods(accessToken);

        Profile profile = new Profile();
        profile.setStatusText(statusText);
        profile.setStatusEmoji(statusEmoji);

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
                .replace("{emoji}", "")
                .replace("{title}", settings.isShowSongTitle() ? songTitle : "")
                .replace("{artist}", settings.isShowArtist() ? artist : "");

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
            throw new RuntimeException("Failed to send Slack message", e);
        }
    }
}
