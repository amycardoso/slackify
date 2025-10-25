package com.trackify.trackify.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "user_settings")
public class UserSettings {

    @Id
    private String id;

    @Indexed(unique = true)
    private String userId;

    @Builder.Default
    private boolean syncEnabled = true;

    @Builder.Default
    private String defaultEmoji = ":musical_note:";

    @Builder.Default
    private boolean notificationsEnabled = false;

    @Builder.Default
    private boolean showArtist = true;

    @Builder.Default
    private boolean showSongTitle = true;

    @Builder.Default
    private String statusTemplate = "{emoji} {title} - {artist}";

    @Builder.Default
    private Map<String, String> genreEmojiMap = new HashMap<>();

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
