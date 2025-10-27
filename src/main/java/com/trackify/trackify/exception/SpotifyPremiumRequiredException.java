package com.trackify.trackify.exception;

public class SpotifyPremiumRequiredException extends SpotifyException {
    public SpotifyPremiumRequiredException() {
        super("Spotify Premium subscription required");
    }

    public SpotifyPremiumRequiredException(String message) {
        super(message);
    }
}
