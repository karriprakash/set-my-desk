package com.setmydesk.ideas.cylinders;

import com.setmydesk.ideas.IdeaContext;
import com.setmydesk.ideas.MeritScorer;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** 7. Score the idea's substance: detail, problem, solution, audience, structure. */
@Component
@Order(7)
public class C07Merit implements IdeaCylinder {
    private final MeritScorer scorer;

    public C07Merit(MeritScorer scorer) {
        this.scorer = scorer;
    }

    public String name() { return "07-merit"; }

    public void fire(IdeaContext ctx) {
        ctx.merit = scorer.score(ctx.text);
    }
}
