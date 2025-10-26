package com.trackify.trackify.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * OAuth state parameter storage for CSRF protection.
 * States are automatically deleted after 10 minutes via MongoDB TTL index.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "oauth_states")
public class OAuthState {

    @Id
    private String id;

    /**
     * The random state parameter used in OAuth flow
     */
    @Indexed(unique = true)
    private String state;

    /**
     * When this state was created.
     * MongoDB TTL index will automatically delete documents 10 minutes after this timestamp.
     */
    @Indexed(expireAfterSeconds = 600) // 10 minutes
    private LocalDateTime createdAt;
}
