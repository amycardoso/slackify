package com.trackify.trackify.controller;

import com.slack.api.bolt.App;
import com.slack.api.bolt.request.Request;
import com.slack.api.bolt.response.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/slack")
@RequiredArgsConstructor
public class SlackController {

    private final App slackApp;

    @PostMapping("/events")
    public ResponseEntity<Map<String, Object>> handleSlackEvents(
            @RequestBody String requestBody,
            @RequestHeader Map<String, String> headers) {
        try {
            log.info("Received Slack event: {}", requestBody);

            // Create Slack Bolt request
            Request<?> req = new Request<>(requestBody, headers);

            // Process the request with Slack Bolt
            Response response = slackApp.run(req);

            log.info("Slack response status: {}", response.getStatusCode());

            return ResponseEntity
                    .status(response.getStatusCode())
                    .body(response.getBody() != null ? Map.of("response", response.getBody()) : Map.of());

        } catch (Exception e) {
            log.error("Error handling Slack event", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
