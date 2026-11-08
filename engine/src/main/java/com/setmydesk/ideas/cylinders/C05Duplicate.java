package com.setmydesk.ideas.cylinders;

import com.setmydesk.ideas.Idea;
import com.setmydesk.ideas.IdeaContext;
import com.setmydesk.ideas.IdeaRepository;
import com.setmydesk.ideas.IdeaStatus;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Locale;

/** 5. The same person sending the same words twice earns once. */
@Component
@Order(5)
public class C05Duplicate implements IdeaCylinder {
    private final IdeaRepository ideas;

    public C05Duplicate(IdeaRepository ideas) {
        this.ideas = ideas;
    }

    public String name() { return "05-duplicate"; }

    public boolean shouldFire(IdeaContext ctx) {
        return ctx.contributor.getId() != null;
    }

    public void fire(IdeaContext ctx) {
        String mine = key(ctx.text);
        for (Idea previous : ideas.findByContributorIdAndStatus(ctx.contributor.getId(), IdeaStatus.ACCEPTED)) {
            if (key(previous.getText()).equals(mine)) {
                ctx.status = IdeaStatus.DUPLICATE;
                ctx.reply = "You've already shared this idea and your shares are safe in project \"" + previous.getProject().getTitle() + "\".";
                ctx.halt("duplicate of idea " + previous.getId());
                return;
            }
        }
    }

    private static String key(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", "");
    }
}
