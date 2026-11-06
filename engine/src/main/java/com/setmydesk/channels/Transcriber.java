package com.setmydesk.channels;

import java.util.Optional;

/** Speech-to-text. Returns empty when unavailable; the idea then waits for a volunteer. */
public interface Transcriber {
    Optional<String> transcribe(String audioPath);
}
