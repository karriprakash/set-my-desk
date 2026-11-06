package com.setmydesk.ideas.cylinders;

import com.setmydesk.ideas.IdeaContext;
import com.setmydesk.ideas.IdeaKind;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** 9. Give a new project a readable title taken from the idea's first sentence. */
@Component
@Order(9)
public class C09Title implements IdeaCylinder {
    static final int MAX = 60;

    public String name() { return "09-title"; }

    public boolean shouldFire(IdeaContext ctx) {
        return ctx.kind == IdeaKind.FOUNDING;
    }

    public void fire(IdeaContext ctx) {
        String first = ctx.text.split("[.!?।\\n]", 2)[0].trim();
        if (first.length() > MAX) {
            int cut = first.lastIndexOf(' ', MAX);
            first = first.substring(0, cut > 20 ? cut : MAX) + "…";
        }
        ctx.projectTitle = first.isEmpty() ? "Untitled idea" : Character.toUpperCase(first.charAt(0)) + first.substring(1);
    }
}
