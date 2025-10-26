package com.trackify.trackify.controller;

import com.slack.api.bolt.App;
import com.slack.api.bolt.jakarta_servlet.SlackAppServlet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class SlackController {

    @Bean
    public ServletRegistrationBean<SlackAppServlet> slackServlet(App app) {
        log.info("Creating Slack servlet registration with app: {}", app != null ? "initialized" : "null");

        SlackAppServlet servlet = new SlackAppServlet(app);
        ServletRegistrationBean<SlackAppServlet> registrationBean =
            new ServletRegistrationBean<>(servlet, "/slack/events");

        registrationBean.setLoadOnStartup(1);

        log.info("Slack servlet registered at /slack/events");
        return registrationBean;
    }
}
