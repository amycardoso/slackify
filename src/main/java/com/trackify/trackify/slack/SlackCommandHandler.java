package com.trackify.trackify.slack;

import com.slack.api.bolt.App;
import com.slack.api.bolt.context.builtin.SlashCommandContext;
import com.slack.api.bolt.request.builtin.SlashCommandRequest;
import com.slack.api.bolt.response.Response;
import com.trackify.trackify.exception.*;
import com.trackify.trackify.model.User;
import com.trackify.trackify.model.UserSettings;
import com.trackify.trackify.service.ErrorMessageService;
import com.trackify.trackify.service.MusicSyncService;
import com.trackify.trackify.service.SpotifyService;
import com.trackify.trackify.service.UserService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SlackCommandHandler {

    private final App slackApp;
    private final UserService userService;
    private final SpotifyService spotifyService;
    private final MusicSyncService musicSyncService;
    private final ErrorMessageService errorMessageService;

    @PostConstruct
    public void registerCommands() {
        // /trackify command router
        slackApp.command("/trackify", (req, ctx) -> {
            String text = req.getPayload().getText().trim();
            String[] parts = text.split("\\s+");
            String subCommand = parts.length > 0 ? parts[0].toLowerCase() : "";

            log.info("Received /trackify command: {} from user: {}",
                    text, req.getPayload().getUserId());

            return switch (subCommand) {
                case "play" -> handlePlay(req, ctx);
                case "pause" -> handlePause(req, ctx);
                case "status" -> handleStatus(req, ctx);
                case "sync" -> handleSync(req, ctx);
                case "enable" -> handleEnable(req, ctx);
                case "disable" -> handleDisable(req, ctx);
                case "reconnect" -> handleReconnect(req, ctx);
                case "help" -> handleHelp(req, ctx);
                default -> handleHelp(req, ctx);
            };
        });

        log.info("Slack slash commands registered successfully");
    }

    private Response handlePlay(SlashCommandRequest req, SlashCommandContext ctx) {
        try {
            String slackUserId = req.getPayload().getUserId();
            Optional<User> userOpt = userService.findBySlackUserId(slackUserId);

            if (userOpt.isEmpty()) {
                return ctx.ack(errorMessageService.buildNotConnectedMessage());
            }

            User user = userOpt.get();
            if (user.getEncryptedSpotifyAccessToken() == null) {
                return ctx.ack(errorMessageService.buildNotConnectedMessage());
            }

            spotifyService.resumePlayback(user);
            return ctx.ack(":arrow_forward: Playback resumed!");

        } catch (NoActiveDeviceException e) {
            return ctx.ack(errorMessageService.buildNoDeviceMessage());
        } catch (SpotifyTokenExpiredException e) {
            return ctx.ack(errorMessageService.buildTokenExpiredMessage(req.getPayload().getUserId()));
        } catch (SpotifyPremiumRequiredException e) {
            return ctx.ack(errorMessageService.buildPremiumRequiredMessage());
        } catch (SpotifyRateLimitException e) {
            return ctx.ack(errorMessageService.buildRateLimitMessage());
        } catch (SpotifyException e) {
            log.error("Spotify error handling play command", e);
            return ctx.ack(errorMessageService.buildNetworkErrorMessage());
        } catch (Exception e) {
            log.error("Unexpected error handling play command", e);
            return ctx.ack(errorMessageService.buildGenericErrorMessage());
        }
    }

    private Response handlePause(SlashCommandRequest req, SlashCommandContext ctx) {
        try {
            String slackUserId = req.getPayload().getUserId();
            Optional<User> userOpt = userService.findBySlackUserId(slackUserId);

            if (userOpt.isEmpty()) {
                return ctx.ack(errorMessageService.buildNotConnectedMessage());
            }

            User user = userOpt.get();
            if (user.getEncryptedSpotifyAccessToken() == null) {
                return ctx.ack(errorMessageService.buildNotConnectedMessage());
            }

            spotifyService.pausePlayback(user);
            return ctx.ack(":pause_button: Playback paused!");

        } catch (NoActiveDeviceException e) {
            return ctx.ack(errorMessageService.buildNoDeviceMessage());
        } catch (SpotifyTokenExpiredException e) {
            return ctx.ack(errorMessageService.buildTokenExpiredMessage(req.getPayload().getUserId()));
        } catch (SpotifyPremiumRequiredException e) {
            return ctx.ack(errorMessageService.buildPremiumRequiredMessage());
        } catch (SpotifyRateLimitException e) {
            return ctx.ack(errorMessageService.buildRateLimitMessage());
        } catch (SpotifyException e) {
            log.error("Spotify error handling pause command", e);
            return ctx.ack(errorMessageService.buildNetworkErrorMessage());
        } catch (Exception e) {
            log.error("Unexpected error handling pause command", e);
            return ctx.ack(errorMessageService.buildGenericErrorMessage());
        }
    }

    private Response handleStatus(SlashCommandRequest req, SlashCommandContext ctx) {
        try {
            String slackUserId = req.getPayload().getUserId();
            Optional<User> userOpt = userService.findBySlackUserId(slackUserId);

            if (userOpt.isEmpty()) {
                return ctx.ack(":x: You need to connect your Spotify account first. Visit /slack/install to get started.");
            }

            User user = userOpt.get();
            Optional<UserSettings> settingsOpt = userService.getUserSettings(user.getId());

            if (settingsOpt.isEmpty()) {
                return ctx.ack(":x: User settings not found.");
            }

            UserSettings settings = settingsOpt.get();
            String statusMessage = buildStatusMessage(user, settings);

            return ctx.ack(statusMessage);

        } catch (Exception e) {
            log.error("Error handling status command", e);
            return ctx.ack(":x: Failed to get status. Error: " + e.getMessage());
        }
    }

    private Response handleSync(SlashCommandRequest req, SlashCommandContext ctx) {
        try {
            String slackUserId = req.getPayload().getUserId();
            musicSyncService.manualSync(slackUserId);
            return ctx.ack(":arrows_counterclockwise: Manual sync triggered!");

        } catch (Exception e) {
            log.error("Error handling sync command", e);
            return ctx.ack(":x: Failed to sync. Error: " + e.getMessage());
        }
    }

    private Response handleEnable(SlashCommandRequest req, SlashCommandContext ctx) {
        try {
            String slackUserId = req.getPayload().getUserId();
            Optional<User> userOpt = userService.findBySlackUserId(slackUserId);

            if (userOpt.isEmpty()) {
                return ctx.ack(":x: User not found.");
            }

            User user = userOpt.get();
            Optional<UserSettings> settingsOpt = userService.getUserSettings(user.getId());

            if (settingsOpt.isEmpty()) {
                return ctx.ack(":x: User settings not found.");
            }

            UserSettings settings = settingsOpt.get();
            settings.setSyncEnabled(true);
            userService.updateUserSettings(settings);

            return ctx.ack(":white_check_mark: Music sync enabled!");

        } catch (Exception e) {
            log.error("Error handling enable command", e);
            return ctx.ack(":x: Failed to enable sync. Error: " + e.getMessage());
        }
    }

    private Response handleDisable(SlashCommandRequest req, SlashCommandContext ctx) {
        try {
            String slackUserId = req.getPayload().getUserId();
            Optional<User> userOpt = userService.findBySlackUserId(slackUserId);

            if (userOpt.isEmpty()) {
                return ctx.ack(":x: User not found.");
            }

            User user = userOpt.get();
            Optional<UserSettings> settingsOpt = userService.getUserSettings(user.getId());

            if (settingsOpt.isEmpty()) {
                return ctx.ack(":x: User settings not found.");
            }

            UserSettings settings = settingsOpt.get();
            settings.setSyncEnabled(false);
            userService.updateUserSettings(settings);

            return ctx.ack(":no_entry: Music sync disabled!");

        } catch (Exception e) {
            log.error("Error handling disable command", e);
            return ctx.ack(":x: Failed to disable sync. Error: " + e.getMessage());
        }
    }

    private Response handleReconnect(SlashCommandRequest req, SlashCommandContext ctx) {
        try {
            String slackUserId = req.getPayload().getUserId();
            Optional<User> userOpt = userService.findBySlackUserId(slackUserId);

            if (userOpt.isEmpty()) {
                return ctx.ack(errorMessageService.buildNotConnectedMessage());
            }

            User user = userOpt.get();
            String reconnectUrl = "/oauth/spotify?userId=" + user.getId();

            return ctx.ack(errorMessageService.buildReconnectInstructions(reconnectUrl));

        } catch (Exception e) {
            log.error("Error handling reconnect command", e);
            return ctx.ack(errorMessageService.buildGenericErrorMessage());
        }
    }

    private Response handleHelp(SlashCommandRequest req, SlashCommandContext ctx) {
        String helpMessage = """
                :musical_note: *Trackify Commands*

                `/trackify play` - Resume Spotify playback
                `/trackify pause` - Pause Spotify playback
                `/trackify status` - Show current sync status
                `/trackify sync` - Manually trigger music sync
                `/trackify enable` - Enable automatic music sync
                `/trackify disable` - Disable automatic music sync
                `/trackify reconnect` - Reconnect your Spotify account
                `/trackify help` - Show this help message

                :link: To get started, connect your accounts at: /slack/install
                """;

        return ctx.ack(helpMessage);
    }

    private String buildStatusMessage(User user, UserSettings settings) {
        StringBuilder message = new StringBuilder();
        message.append(":musical_note: *Your Trackify Status*\n\n");

        // Sync status
        message.append("*Sync Status:* ");
        message.append(settings.isSyncEnabled() ? ":white_check_mark: Enabled" : ":no_entry: Disabled");
        message.append("\n");

        // Spotify connection
        message.append("*Spotify:* ");
        message.append(user.getEncryptedSpotifyAccessToken() != null ?
                ":white_check_mark: Connected" : ":x: Not connected");
        message.append("\n");

        // Currently playing
        if (user.getCurrentlyPlayingSongTitle() != null) {
            message.append("*Now Playing:* ");
            message.append(user.getCurrentlyPlayingSongTitle());
            message.append(" - ");
            message.append(user.getCurrentlyPlayingArtist());
            message.append("\n");
        } else {
            message.append("*Now Playing:* Nothing\n");
        }

        // Settings
        message.append("\n*Settings:*\n");
        message.append("• Emoji: ").append(settings.getDefaultEmoji()).append("\n");
        message.append("• Show Artist: ").append(settings.isShowArtist() ? "Yes" : "No").append("\n");
        message.append("• Show Title: ").append(settings.isShowSongTitle() ? "Yes" : "No").append("\n");
        message.append("• Notifications: ").append(settings.isNotificationsEnabled() ? "Enabled" : "Disabled");

        return message.toString();
    }
}
