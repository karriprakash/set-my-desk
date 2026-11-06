package com.setmydesk.ideas.cylinders;

import com.setmydesk.channels.Transcriber;
import com.setmydesk.ideas.IdeaContext;
import com.setmydesk.ideas.IdeaStatus;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** 2. Voice note only? Turn speech into text, or park it for a volunteer transcriber. */
@Component
@Order(2)
public class C02Transcription implements IdeaCylinder {
    private final Transcriber transcriber;

    public C02Transcription(Transcriber transcriber) {
        this.transcriber = transcriber;
    }

    public String name() { return "02-transcription"; }

    public boolean shouldFire(IdeaContext ctx) {
        return !ctx.hasText() && ctx.audioPath != null;
    }

    public void fire(IdeaContext ctx) {
        Optional<String> text = transcriber.transcribe(ctx.audioPath);
        if (text.isPresent() && !text.get().isBlank()) {
            ctx.text = text.get().trim();
            return;
        }
        ctx.status = IdeaStatus.PENDING_TRANSCRIPTION;
        ctx.reply = "Thank you! We received your voice note. A volunteer will write it down and you'll get your shares as soon as it's processed.";
        ctx.halt("awaiting transcription");
    }
}
