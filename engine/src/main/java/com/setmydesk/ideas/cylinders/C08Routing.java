package com.setmydesk.ideas.cylinders;

import com.setmydesk.core.EngineProperties;
import com.setmydesk.ideas.IdeaContext;
import com.setmydesk.ideas.IdeaKind;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** 8. Already exists? It becomes an enhancement of that project. Otherwise it founds a new one. */
@Component
@Order(8)
public class C08Routing implements IdeaCylinder {
    private final EngineProperties props;

    public C08Routing(EngineProperties props) {
        this.props = props;
    }

    public String name() { return "08-routing"; }

    public void fire(IdeaContext ctx) {
        if (ctx.matchedProject != null && ctx.similarity >= props.enhancementThreshold()) {
            ctx.kind = IdeaKind.ENHANCEMENT;
            ctx.project = ctx.matchedProject;
        } else {
            ctx.kind = IdeaKind.FOUNDING;
        }
    }
}
