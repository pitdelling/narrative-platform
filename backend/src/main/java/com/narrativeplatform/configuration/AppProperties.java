package com.narrativeplatform.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String frontendUrl,
        String publicUrl,
        String jwtSecret,
        long jwtExpirationHours,
        long turnExpirationHours,
        long writtenLockExpirationHours,
        int narratorRevealSeconds,
        OpenAi openai
) {
    public record OpenAi(String apiKey, String model, String baseUrl) {
        public boolean configured() {
            return apiKey != null && !apiKey.isBlank();
        }
    }
}
