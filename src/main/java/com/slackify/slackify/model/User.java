package com.slackify.slackify.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String slackUserId;

    private String slackTeamId;

    private String slackAccessToken;

    private String spotifyUserId;

    private String encryptedSpotifyAccessToken;

    private String encryptedSpotifyRefreshToken;

    private LocalDateTime spotifyTokenExpiresAt;

    private String currentlyPlayingSongId;

    private String currentlyPlayingSongTitle;

    private String currentlyPlayingArtist;

    private LocalDateTime lastSyncedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private boolean active;
}
