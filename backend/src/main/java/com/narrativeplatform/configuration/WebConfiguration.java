package com.narrativeplatform.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration(proxyBeanMethods = false)
public class WebConfiguration {

    @Bean
    CorsConfigurationSource corsConfigurationSource(final AppProperties properties) {
        final var configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(properties.frontendUrl()));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Lock-Token"));
        configuration.setExposedHeaders(List.of("Location"));
        configuration.setAllowCredentials(false);
        final var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
