package com.setmydesk.channels;

import com.setmydesk.core.EngineProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Constant-time check of the shared webhook / volunteer tokens. */
@Component
public class WebhookGuard {

    private final EngineProperties props;

    public WebhookGuard(EngineProperties props) {
        this.props = props;
    }

    public void requireWebhook(String token) {
        require(props.webhookToken(), token);
    }

    public void requireVolunteer(String token) {
        require(props.volunteerToken(), token);
    }

    private static void require(String expected, String given) {
        if (expected == null || given == null
                || !MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), given.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid token");
        }
    }
}
