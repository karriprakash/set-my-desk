package com.setmydesk.ideas.cylinders;

import com.setmydesk.ideas.IdeaContext;
import com.setmydesk.ideas.IdeaKind;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** 12. Tell the contributor, in plain words, what happened and what she earned. */
@Component
@Order(12)
public class C12Reply implements IdeaCylinder {

    public String name() { return "12-reply"; }

    public void fire(IdeaContext ctx) {
        double percent = ctx.project.getTotalPoints() == 0 ? 0 : 100 * ctx.points / ctx.project.getTotalPoints();
        if (ctx.kind == IdeaKind.FOUNDING) {
            ctx.reply = "Wonderful, %s! Your idea started a new project: \"%s\". You earned %.0f points (100%% of the project today). Project #%d."
                    .formatted(ctx.contributor.getDisplayName(), ctx.project.getTitle(), ctx.points, ctx.project.getId());
        } else {
            ctx.reply = "Thank you, %s! Something similar already exists, so your idea now improves the project \"%s\". You earned %.0f points (%.1f%% of the project today). Project #%d."
                    .formatted(ctx.contributor.getDisplayName(), ctx.project.getTitle(), ctx.points, percent, ctx.project.getId());
        }
    }
}
