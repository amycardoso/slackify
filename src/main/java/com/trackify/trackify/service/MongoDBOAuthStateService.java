package com.trackify.trackify.service;

import com.slack.api.bolt.service.OAuthStateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class MongoDBOAuthStateService implements OAuthStateService {

    // In-memory storage for OAuth state (valid for ~10 minutes)
    // For production, you might want to use Redis or MongoDB with TTL
    private final Map<String, Long> stateStore = new ConcurrentHashMap<>();
    private static final long STATE_EXPIRATION_MS = 10 * 60 * 1000; // 10 minutes

    @Override
    public void addNewStateToDatastore(String state) throws Exception {
        log.debug("Storing OAuth state: {}", state);
        stateStore.put(state, System.currentTimeMillis());

        // Clean up expired states
        cleanupExpiredStates();
    }

    @Override
    public boolean isAvailableInDatabase(String state) {
        log.debug("Checking OAuth state: {}", state);

        Long timestamp = stateStore.get(state);
        if (timestamp == null) {
            log.warn("OAuth state not found: {}", state);
            return false;
        }

        // Check if expired
        if (System.currentTimeMillis() - timestamp > STATE_EXPIRATION_MS) {
            log.warn("OAuth state expired: {}", state);
            stateStore.remove(state);
            return false;
        }

        return true;
    }

    @Override
    public void deleteStateFromDatastore(String state) throws Exception {
        log.debug("Deleting OAuth state: {}", state);
        stateStore.remove(state);
    }

    private void cleanupExpiredStates() {
        long now = System.currentTimeMillis();
        stateStore.entrySet().removeIf(entry ->
            now - entry.getValue() > STATE_EXPIRATION_MS
        );
    }
}
