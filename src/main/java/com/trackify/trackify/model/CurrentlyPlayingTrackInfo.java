package com.trackify.trackify.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CurrentlyPlayingTrackInfo {
    private String trackId;
    private String trackName;
    private String artistName;
    private boolean isPlaying;
    private Integer durationMs; // Track duration in milliseconds
}
