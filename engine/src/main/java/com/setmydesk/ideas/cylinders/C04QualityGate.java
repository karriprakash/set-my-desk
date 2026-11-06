package com.setmydesk.ideas.cylinders;

import com.setmydesk.core.TextSimilarity;
import com.setmydesk.ideas.IdeaContext;
import com.setmydesk.ideas.IdeaStatus;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** 4. Need at least a few meaningful words to judge an idea fairly; otherwise ask kindly for more. */
@Component
@Order(4)
public class C04QualityGate implements IdeaCylinder {
    static final int MIN_MEANINGFUL_WORDS = 4;

    public String name() { return "04-quality-gate"; }

    public void fire(IdeaContext ctx) {
        if (TextSimilarity.tokens(ctx.text).size() < MIN_MEANINGFUL_WORDS) {
            ctx.status = IdeaStatus.NEEDS_DETAIL;
            ctx.reply = "Thanks! Could you tell us a little more? Who has the problem, what the problem is, and how your idea solves it. Then send it again.";
            ctx.halt("too short");
        }
    }
}
