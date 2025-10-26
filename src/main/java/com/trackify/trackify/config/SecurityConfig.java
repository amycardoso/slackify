package com.trackify.trackify.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/slack/**", "/oauth/**", "/api/**", "/health")
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/oauth/**", "/slack/**", "/health", "/", "/success", "/error", "/dashboard").permitAll()
                        .anyRequest().authenticated()
                );

        return http.build();
    }

    // Completely bypass Spring Security for Slack events
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers("/slack/events");
    }
}
