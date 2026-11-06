package com.setmydesk.channels;

import com.setmydesk.core.EngineProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

/**
 * Calls any OpenAI-compatible {@code /audio/transcriptions} endpoint (hosted Whisper, a self-hosted
 * faster-whisper server, etc.). Disabled unless {@code setmydesk.transcription.url} is set.
 */
@Component
public class HttpTranscriber implements Transcriber {

    private static final Logger log = LoggerFactory.getLogger(HttpTranscriber.class);
    private final EngineProperties.Transcription config;
    private final RestClient http = RestClient.create();

    public HttpTranscriber(EngineProperties props) {
        this.config = props.transcription();
    }

    @Override
    public Optional<String> transcribe(String audioPath) {
        if (!config.enabled() || audioPath == null || !Files.exists(Path.of(audioPath))) return Optional.empty();
        try {
            var form = new LinkedMultiValueMap<String, Object>();
            form.add("file", new FileSystemResource(audioPath));
            form.add("model", config.model());
            var request = http.post().uri(config.url()).contentType(MediaType.MULTIPART_FORM_DATA).body(form);
            if (config.apiKey() != null && !config.apiKey().isBlank()) request = request.header("Authorization", "Bearer " + config.apiKey());
            Map<?, ?> body = request.retrieve().body(Map.class);
            return Optional.ofNullable(body == null ? null : (String) body.get("text"));
        } catch (Exception e) {
            log.warn("Transcription failed for {}: {}", audioPath, e.getMessage());
            return Optional.empty();
        }
    }
}
