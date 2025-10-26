package com.trackify.trackify.controller;

import com.slack.api.bolt.App;
import com.slack.api.bolt.request.Request;
import com.slack.api.bolt.response.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/slack")
@RequiredArgsConstructor
public class SlackController {

    private final App slackApp;

    @PostMapping(value = "/events", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<String> handleSlackEvents(
            @RequestBody String requestBody,
            @RequestHeader Map<String, String> headers) {
        try {
            log.info("Received Slack event");
            log.debug("Request body: {}", requestBody);
            log.debug("Headers: {}", headers);

            // Convert header keys to lowercase for compatibility
            Map<String, String> normalizedHeaders = new HashMap<>();
            headers.forEach((key, value) -> normalizedHeaders.put(key.toLowerCase(), value));

            // Create Slack Bolt request
            Request<?> req = new Request<>(requestBody, normalizedHeaders);

            // Process the request with Slack Bolt
            Response response = slackApp.run(req);

            log.info("Slack response status: {}, body: {}", response.getStatusCode(), response.getBody());

            // Return the response body as plain text or JSON
            String responseBody = response.getBody() != null ? response.getBody() : "";

            return ResponseEntity
                    .status(response.getStatusCode())
                    .contentType(response.getContentType() != null ?
                        MediaType.parseMediaType(response.getContentType()) :
                        MediaType.APPLICATION_JSON)
                    .body(responseBody);

        } catch (Exception e) {
            log.error("Error handling Slack event", e);
            return ResponseEntity
                    .status(500)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}
