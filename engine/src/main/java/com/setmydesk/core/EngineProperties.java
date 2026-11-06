package com.setmydesk.core;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** All tunables of the engine, bound from {@code setmydesk.*} properties. */
@ConfigurationProperties(prefix = "setmydesk")
public record EngineProperties(
        String audioDir,
        double enhancementThreshold,
        double basePoints,
        String webhookToken,
        String volunteerToken,
        Transcription transcription,
        List<String> corsOrigins) {

    public record Transcription(String url, String apiKey, String model) {
        public boolean enabled() {
            return url != null && !url.isBlank();
        }
    }

    public EngineProperties {
        if (transcription == null) transcription = new Transcription("", "", "whisper-1");
        if (corsOrigins == null) corsOrigins = List.of();
        if (volunteerToken == null || volunteerToken.isBlank()) volunteerToken = webhookToken;
    }
}
