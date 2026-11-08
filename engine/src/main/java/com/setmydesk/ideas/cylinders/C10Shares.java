package com.setmydesk.ideas.cylinders;

import com.setmydesk.core.EngineProperties;
import com.setmydesk.ideas.IdeaContext;
import com.setmydesk.ideas.IdeaKind;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 10. points = basePoints × merit × novelty.
 * <p>
 * Founding idea: novelty = 1. Enhancement: novelty = 1 − similarity, so adding something genuinely new
 * to an existing project earns more than repeating it. The formula has no time input at all: the same
 * idea earns the same points on day one or year five.
 */
@Component
@Order(10)
public class C10Shares implements IdeaCylinder {
    private final EngineProperties props;

    public C10Shares(EngineProperties props) {
        this.props = props;
    }

    public String name() { return "10-shares"; }

    public void fire(IdeaContext ctx) {
        ctx.novelty = ctx.kind == IdeaKind.FOUNDING ? 1.0 : Math.max(0.0, 1.0 - ctx.similarity);
        ctx.points = Math.round(props.basePoints() * ctx.merit.total() * ctx.novelty * 100) / 100.0;
    }
}
