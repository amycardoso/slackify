package com.trackify.trackify.service;

import com.slack.api.bolt.service.OAuthStateService;
import com.trackify.trackify.model.OAuthState;
import com.trackify.trackify.repository.OAuthStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * MongoDB-based OAuth state service for CSRF protection.
 * States are automatically expired after 10 minutes via MongoDB TTL index.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MongoDBOAuthStateService implements OAuthStateService {

    private final OAuthStateRepository oauthStateRepository;

    @Override
    public void addNewStateToDatastore(String state) throws Exception {
        log.debug("Storing OAuth state in MongoDB: {}", state);

        OAuthState oauthState = OAuthState.builder()
                .state(state)
                .createdAt(LocalDateTime.now())
                .build();

        oauthStateRepository.save(oauthState);
        log.info("OAuth state stored successfully: {}", state);
    }

    @Override
    public boolean isAvailableInDatabase(String state) {
        log.debug("Checking OAuth state in MongoDB: {}", state);

        boolean exists = oauthStateRepository.existsByState(state);

        if (!exists) {
            log.warn("OAuth state not found or expired: {}", state);
            return false;
        }

        log.info("OAuth state validated successfully: {}", state);
        return true;
    }

    @Override
    public void deleteStateFromDatastore(String state) throws Exception {
        log.debug("Deleting OAuth state from MongoDB: {}", state);
        oauthStateRepository.deleteByState(state);
        log.info("OAuth state deleted: {}", state);
    }
}
