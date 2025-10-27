package com.trackify.trackify.constants;

public final class AppConstants {

    private AppConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // OAuth paths
    public static final String OAUTH_SPOTIFY_PATH = "/oauth/spotify";
    public static final String OAUTH_SPOTIFY_CALLBACK_PATH = "/oauth/spotify/callback";
    public static final String SLACK_INSTALL_PATH = "/slack/install";

    // Error message keys
    public static final String ERROR_USER_NOT_FOUND = "User not found";
    public static final String ERROR_USER_SETTINGS_NOT_FOUND = "User settings not found";
    public static final String ERROR_FAILED_TO_UPDATE_SLACK_STATUS = "Failed to update Slack status";
    public static final String ERROR_FAILED_TO_CLEAR_SLACK_STATUS = "Failed to clear Slack status";
    public static final String ERROR_FAILED_TO_SEND_SLACK_MESSAGE = "Failed to send Slack message";

    // Spotify
    public static final String SPOTIFY_GREEN_COLOR = "#1DB954";
    public static final String SPOTIFY_USER_ID_PREFIX = "spotify_user_";
    public static final String UNKNOWN_ARTIST = "Unknown Artist";

    // Template placeholders
    public static final String PLACEHOLDER_EMOJI = "{emoji}";
    public static final String PLACEHOLDER_TITLE = "{title}";
    public static final String PLACEHOLDER_ARTIST = "{artist}";
}
