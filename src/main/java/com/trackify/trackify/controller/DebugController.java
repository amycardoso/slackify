package com.trackify.trackify.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class DebugController {

    private final Environment environment;

    public DebugController(Environment environment) {
        this.environment = environment;
    }

    @GetMapping("/debug/env")
    public Map<String, String> checkEnvironment() {
        Map<String, String> envCheck = new HashMap<>();

        // Check if environment variables are accessible
        envCheck.put("SLACK_CLIENT_ID_exists", System.getenv("SLACK_CLIENT_ID") != null ? "YES" : "NO");
        envCheck.put("SLACK_CLIENT_SECRET_exists", System.getenv("SLACK_CLIENT_SECRET") != null ? "YES" : "NO");
        envCheck.put("SLACK_SIGNING_SECRET_exists", System.getenv("SLACK_SIGNING_SECRET") != null ? "YES" : "NO");
        envCheck.put("SPOTIFY_CLIENT_ID_exists", System.getenv("SPOTIFY_CLIENT_ID") != null ? "YES" : "NO");
        envCheck.put("SPOTIFY_CLIENT_SECRET_exists", System.getenv("SPOTIFY_CLIENT_SECRET") != null ? "YES" : "NO");
        envCheck.put("SPRING_DATA_MONGODB_URI_exists", System.getenv("SPRING_DATA_MONGODB_URI") != null ? "YES" : "NO");
        envCheck.put("ENCRYPTION_SECRET_KEY_exists", System.getenv("ENCRYPTION_SECRET_KEY") != null ? "YES" : "NO");
        envCheck.put("PORT_exists", System.getenv("PORT") != null ? "YES" : "NO");

        // Check Spring Environment
        envCheck.put("spring_slack_client_id_resolved", environment.getProperty("slack.client-id", "NOT_FOUND"));
        envCheck.put("spring_slack_client_secret_resolved", environment.getProperty("slack.client-secret") != null ? "YES" : "NO");

        // Show first 4 chars of values (for debugging)
        String slackClientId = System.getenv("SLACK_CLIENT_ID");
        if (slackClientId != null && slackClientId.length() > 4) {
            envCheck.put("SLACK_CLIENT_ID_preview", slackClientId.substring(0, 4) + "...");
        }

        return envCheck;
    }
}
