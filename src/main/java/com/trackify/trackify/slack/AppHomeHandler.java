package com.trackify.trackify.slack;

import com.slack.api.bolt.App;
import com.slack.api.model.event.AppHomeOpenedEvent;
import com.slack.api.model.view.View;
import com.trackify.trackify.constants.AppConstants;
import com.trackify.trackify.model.User;
import com.trackify.trackify.model.UserSettings;
import com.trackify.trackify.service.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

import static com.slack.api.model.block.Blocks.*;
import static com.slack.api.model.block.composition.BlockCompositions.*;
import static com.slack.api.model.block.element.BlockElements.*;
import static com.slack.api.model.view.Views.*;

/**
 * Handles Slack App Home events and interactions.
 * Registers event listeners for App Home tab and button actions.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppHomeHandler {

    private final App slackApp;
    private final AppHomeService appHomeService;
    private final UserService userService;
    private final MusicSyncService musicSyncService;
    private final WorkingHoursValidator workingHoursValidator;
    private final TimezoneService timezoneService;

    @PostConstruct
    public void registerHandlers() {
        registerAppHomeOpenedEvent();
        registerEnableSyncAction();
        registerDisableSyncAction();
        registerManualSyncAction();
        registerConfigureWorkingHoursAction();
        registerWorkingHoursModalSubmission();
        registerReconnectSpotifyAction();

        log.info("App Home handlers registered successfully");
    }

    /**
     * Handles app_home_opened event - publishes the home view when user opens App Home tab.
     */
    private void registerAppHomeOpenedEvent() {
        slackApp.event(AppHomeOpenedEvent.class, (payload, ctx) -> {
            try {
                String userId = payload.getEvent().getUser();
                log.debug("App Home opened by user: {}", userId);

                // Get the bot token from installation to publish view
                String botToken = ctx.getBotToken();
                appHomeService.publishHomeView(userId, botToken);

                return ctx.ack();
            } catch (Exception e) {
                log.error("Error handling app_home_opened event", e);
                return ctx.ack();
            }
        });
    }

    /**
     * Handles "Enable Sync" button click.
     */
    private void registerEnableSyncAction() {
        slackApp.blockAction("enable_sync", (req, ctx) -> {
            try {
                String userId = req.getPayload().getUser().getId();
                log.info("User {} clicked Enable Sync", userId);

                Optional<User> userOpt = userService.findBySlackUserId(userId);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    Optional<UserSettings> settingsOpt = userService.getUserSettings(user.getId());

                    if (settingsOpt.isPresent()) {
                        UserSettings settings = settingsOpt.get();
                        settings.setSyncEnabled(true);
                        userService.updateUserSettings(settings);

                        // Refresh home view
                        appHomeService.publishHomeView(userId, ctx.getBotToken());
                    }
                }

                return ctx.ack();
            } catch (Exception e) {
                log.error("Error handling enable_sync action", e);
                return ctx.ack();
            }
        });
    }

    /**
     * Handles "Disable Sync" button click.
     */
    private void registerDisableSyncAction() {
        slackApp.blockAction("disable_sync", (req, ctx) -> {
            try {
                String userId = req.getPayload().getUser().getId();
                log.info("User {} clicked Disable Sync", userId);

                Optional<User> userOpt = userService.findBySlackUserId(userId);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    Optional<UserSettings> settingsOpt = userService.getUserSettings(user.getId());

                    if (settingsOpt.isPresent()) {
                        UserSettings settings = settingsOpt.get();
                        settings.setSyncEnabled(false);
                        userService.updateUserSettings(settings);

                        // Refresh home view
                        appHomeService.publishHomeView(userId, ctx.getBotToken());
                    }
                }

                return ctx.ack();
            } catch (Exception e) {
                log.error("Error handling disable_sync action", e);
                return ctx.ack();
            }
        });
    }

    /**
     * Handles "Sync Now" button click - triggers manual sync.
     */
    private void registerManualSyncAction() {
        slackApp.blockAction("manual_sync", (req, ctx) -> {
            try {
                String userId = req.getPayload().getUser().getId();
                log.info("User {} clicked Sync Now", userId);

                musicSyncService.manualSync(userId);

                // Refresh home view to show updated "Now Playing"
                appHomeService.publishHomeView(userId, ctx.getBotToken());

                return ctx.ack();
            } catch (Exception e) {
                log.error("Error handling manual_sync action", e);
                return ctx.ack();
            }
        });
    }

    /**
     * Handles "Configure Working Hours" button click - opens modal.
     */
    private void registerConfigureWorkingHoursAction() {
        slackApp.blockAction("configure_working_hours", (req, ctx) -> {
            try {
                String userId = req.getPayload().getUser().getId();
                log.info("User {} clicked Configure Working Hours", userId);

                Optional<User> userOpt = userService.findBySlackUserId(userId);
                if (userOpt.isEmpty()) {
                    return ctx.ack();
                }

                User user = userOpt.get();
                Optional<UserSettings> settingsOpt = userService.getUserSettings(user.getId());
                if (settingsOpt.isEmpty()) {
                    return ctx.ack();
                }

                UserSettings settings = settingsOpt.get();
                View modalView = buildWorkingHoursModal(settings);

                // Open modal
                ctx.client().viewsOpen(r -> r
                        .triggerId(req.getPayload().getTriggerId())
                        .view(modalView)
                );

                return ctx.ack();
            } catch (Exception e) {
                log.error("Error handling configure_working_hours action", e);
                return ctx.ack();
            }
        });
    }

    /**
     * Builds the working hours configuration modal.
     */
    private View buildWorkingHoursModal(UserSettings settings) {
        // Convert UTC hours to local time for display
        final String startTimeLocal;
        final String endTimeLocal;

        if (settings.getSyncStartHour() != null &&
            settings.getSyncEndHour() != null &&
            settings.getTimezoneOffsetSeconds() != null) {
            startTimeLocal = timezoneService.convertUtcToLocal(
                    settings.getSyncStartHour(),
                    settings.getTimezoneOffsetSeconds());
            endTimeLocal = timezoneService.convertUtcToLocal(
                    settings.getSyncEndHour(),
                    settings.getTimezoneOffsetSeconds());
        } else {
            startTimeLocal = "09:00"; // Default
            endTimeLocal = "17:00"; // Default
        }

        return view(view -> view
                .type("modal")
                .callbackId("working_hours_modal")
                .title(viewTitle(title -> title.type("plain_text").text("Working Hours")))
                .submit(viewSubmit(submit -> submit.type("plain_text").text("Save")))
                .close(viewClose(close -> close.type("plain_text").text("Cancel")))
                .blocks(asBlocks(
                        section(section -> section.text(markdownText(
                                "*Configure your working hours*\n\n" +
                                "Trackify will only update your status during these hours. " +
                                "Times are in your local timezone."
                        ))),
                        input(input -> input
                                .blockId("working_hours_enabled")
                                .label(plainText("Enable Working Hours"))
                                .element(checkboxes(checkboxes -> checkboxes
                                        .actionId("enabled_checkbox")
                                        .options(asOptions(
                                                option(option -> option
                                                        .value("enabled")
                                                        .text(plainText("Only sync during working hours"))
                                                )
                                        ))
                                        .initialOptions(settings.isWorkingHoursEnabled() ?
                                                asOptions(option(option -> option.value("enabled").text(plainText("Only sync during working hours")))) :
                                                asOptions())
                                ))
                                .optional(true)
                        ),
                        input(input -> input
                                .blockId("start_time")
                                .label(plainText("Start Time (your local time)"))
                                .element(timePicker(timePicker -> timePicker
                                        .actionId("start_time_picker")
                                        .initialTime(startTimeLocal)
                                ))
                        ),
                        input(input -> input
                                .blockId("end_time")
                                .label(plainText("End Time (your local time)"))
                                .element(timePicker(timePicker -> timePicker
                                        .actionId("end_time_picker")
                                        .initialTime(endTimeLocal)
                                ))
                        ),
                        context(context -> context.elements(asContextElements(
                                markdownText(":information_source: Times will be converted to UTC for accurate synchronization")
                        )))
                ))
        );
    }

    /**
     * Handles working hours modal submission.
     */
    private void registerWorkingHoursModalSubmission() {
        slackApp.viewSubmission("working_hours_modal", (req, ctx) -> {
            try {
                String userId = req.getPayload().getUser().getId();
                log.info("User {} submitted working hours modal", userId);

                Map<String, Map<String, com.slack.api.model.view.ViewState.Value>> stateValues =
                        req.getPayload().getView().getState().getValues();

                // Extract enabled checkbox
                boolean workingHoursEnabled = false;
                if (stateValues.containsKey("working_hours_enabled")) {
                    var enabledValue = stateValues.get("working_hours_enabled").get("enabled_checkbox");
                    if (enabledValue != null && enabledValue.getSelectedOptions() != null &&
                        !enabledValue.getSelectedOptions().isEmpty()) {
                        workingHoursEnabled = true;
                    }
                }

                // Extract times
                String startTime = stateValues.get("start_time").get("start_time_picker").getSelectedTime();
                String endTime = stateValues.get("end_time").get("end_time_picker").getSelectedTime();

                log.debug("Working hours submission: enabled={}, start={}, end={}",
                        workingHoursEnabled, startTime, endTime);

                // Get user and settings
                Optional<User> userOpt = userService.findBySlackUserId(userId);
                if (userOpt.isEmpty()) {
                    return ctx.ack();
                }

                User user = userOpt.get();
                Optional<UserSettings> settingsOpt = userService.getUserSettings(user.getId());
                if (settingsOpt.isEmpty()) {
                    return ctx.ack();
                }

                UserSettings settings = settingsOpt.get();

                // Validate and convert times
                Integer timezoneOffset = settings.getTimezoneOffsetSeconds();
                if (timezoneOffset == null) {
                    log.warn("User {} has no timezone offset, cannot configure working hours", userId);
                    return ctx.ack(r -> r.responseAction("errors")
                            .errors(Map.of("start_time", "Timezone not available. Please reconnect your account.")));
                }

                Integer[] convertedTimes = workingHoursValidator.validateAndConvert(
                        startTime, endTime, timezoneOffset);

                if (convertedTimes == null) {
                    // Validation failed - same start and end time
                    return ctx.ack(r -> r.responseAction("errors")
                            .errors(Map.of("end_time", "Start and end times cannot be the same")));
                }

                // Save settings
                userService.updateWorkingHours(
                        user.getId(),
                        convertedTimes[0], // UTC start hour
                        convertedTimes[1], // UTC end hour
                        workingHoursEnabled
                );

                log.info("Updated working hours for user {}: enabled={}, {}:{} - {}:{} UTC",
                        userId, workingHoursEnabled,
                        convertedTimes[0] / 100, convertedTimes[0] % 100,
                        convertedTimes[1] / 100, convertedTimes[1] % 100);

                // Refresh home view
                appHomeService.publishHomeView(userId, ctx.getBotToken());

                return ctx.ack();
            } catch (Exception e) {
                log.error("Error handling working hours modal submission", e);
                return ctx.ack();
            }
        });
    }

    /**
     * Handles "Reconnect Spotify" button click for invalidated users.
     */
    private void registerReconnectSpotifyAction() {
        slackApp.blockAction("reconnect_spotify", (req, ctx) -> {
            try {
                String userId = req.getPayload().getUser().getId();
                log.info("User {} clicked Reconnect Spotify", userId);

                Optional<User> userOpt = userService.findBySlackUserId(userId);
                if (userOpt.isEmpty()) {
                    return ctx.ack();
                }

                User user = userOpt.get();

                // Send reconnect instructions via DM
                String reconnectUrl = AppConstants.OAUTH_SPOTIFY_PATH + "?userId=" + user.getId();
                String message = String.format(
                        ":warning: *Your Spotify connection needs to be renewed*\n\n" +
                        "Please click the link below to reconnect your Spotify account:\n\n" +
                        "<%s|Reconnect Spotify Account>",
                        reconnectUrl
                );

                ctx.client().chatPostMessage(r -> r
                        .channel(userId)
                        .text(message)
                );

                return ctx.ack();
            } catch (Exception e) {
                log.error("Error handling reconnect_spotify action", e);
                return ctx.ack();
            }
        });
    }
}
