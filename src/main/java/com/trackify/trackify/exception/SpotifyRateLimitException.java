package com.trackify.trackify.exception;

public class SpotifyRateLimitException extends SpotifyException {
    public SpotifyRateLimitException() {
        super("Spotify API rate limit exceeded");
    }

    public SpotifyRateLimitException(String message) {
        super(message);
    }
}
