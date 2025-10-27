package com.trackify.trackify.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthTemplateService {

    private final ResourceLoader resourceLoader;

    public String renderSuccess(String spotifyAuthLink) {
        try {
            String template = loadTemplate("classpath:templates/slack-oauth-success.html");
            return template.replace("{{SPOTIFY_AUTH_LINK}}", spotifyAuthLink);
        } catch (IOException e) {
            log.error("Failed to load success template", e);
            return getFallbackSuccessHtml(spotifyAuthLink);
        }
    }

    public String renderError(String errorTitle, String errorDescription, String errorDetails) {
        try {
            String template = loadTemplate("classpath:templates/slack-oauth-error.html");
            return template
                    .replace("{{ERROR_TITLE}}", errorTitle)
                    .replace("{{ERROR_DESCRIPTION}}", errorDescription)
                    .replace("{{ERROR_DETAILS}}", errorDetails);
        } catch (IOException e) {
            log.error("Failed to load error template", e);
            return getFallbackErrorHtml(errorTitle, errorDetails);
        }
    }

    private String loadTemplate(String location) throws IOException {
        Resource resource = resourceLoader.getResource(location);
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    private String getFallbackSuccessHtml(String spotifyAuthLink) {
        return """
                <html>
                <body style="font-family: sans-serif; text-align: center; padding: 50px;">
                    <h1>Slack Connected!</h1>
                    <p>Now connect your Spotify account:</p>
                    <a href="%s" style="display: inline-block; padding: 10px 20px; background: #1DB954; color: white; text-decoration: none; border-radius: 5px;">Connect Spotify</a>
                </body>
                </html>
                """.formatted(spotifyAuthLink);
    }

    private String getFallbackErrorHtml(String errorTitle, String errorDetails) {
        return """
                <html>
                <body style="font-family: sans-serif; text-align: center; padding: 50px;">
                    <h1>%s</h1>
                    <p>Error: %s</p>
                    <a href="/slack/install" style="display: inline-block; padding: 10px 20px; background: #1DB954; color: white; text-decoration: none; border-radius: 5px;">Try Again</a>
                </body>
                </html>
                """.formatted(errorTitle, errorDetails);
    }
}
