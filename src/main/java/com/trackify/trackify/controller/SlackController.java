package com.trackify.trackify.controller;

import com.slack.api.bolt.App;
import com.slack.api.bolt.servlet.SlackAppServlet;
import jakarta.servlet.annotation.WebServlet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class SlackController {

    @Autowired
    private App slackApp;

    @Bean
    public ServletRegistrationBean<SlackAppServlet> slackServletRegistration() {
        log.info("Registering Slack servlet at /slack/events");

        SlackAppServlet servlet = new SlackAppServlet(slackApp);
        ServletRegistrationBean<SlackAppServlet> registration =
            new ServletRegistrationBean<>(servlet, "/slack/events");

        registration.setName("SlackAppServlet");

        return registration;
    }
}
