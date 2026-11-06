package com.setmydesk.ideas;

public enum IdeaStatus {
    /** Voice note received, waiting for speech-to-text or a volunteer transcriber. */
    PENDING_TRANSCRIPTION,
    /** Went through all cylinders; shares awarded. */
    ACCEPTED,
    /** Same contributor already sent the same idea. */
    DUPLICATE,
    /** Too short or empty; contributor was asked for more detail. */
    NEEDS_DETAIL
}
