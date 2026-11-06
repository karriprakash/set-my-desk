package com.setmydesk.ideas.cylinders;

import com.setmydesk.ideas.IdeaContext;
import com.setmydesk.ideas.IdeaStatus;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** 1. Normalise whitespace; refuse a submission with neither words nor a voice note. */
@Component
@Order(1)
public class C01Intake implements IdeaCylinder {
    public String name() { return "01-intake"; }

    public void fire(IdeaContext ctx) {
        if (ctx.text != null) ctx.text = ctx.text.replaceAll("[\\t\\x0B\\f\\r ]+", " ").replaceAll("\\n{3,}", "\n\n").trim();
        if (!ctx.hasText() && ctx.audioPath == null) {
            ctx.status = IdeaStatus.NEEDS_DETAIL;
            ctx.reply = "We didn't receive any words or audio. Please send your idea again — typing, a voice note or a call all work.";
            ctx.halt("empty");
        }
    }
}
