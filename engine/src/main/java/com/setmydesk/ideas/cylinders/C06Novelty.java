package com.setmydesk.ideas.cylinders;

import com.setmydesk.core.TextSimilarity;
import com.setmydesk.ideas.IdeaContext;
import com.setmydesk.ideas.Project;
import com.setmydesk.ideas.ProjectRepository;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/** 6. Compare against every existing project and find the closest one. */
@Component
@Order(6)
public class C06Novelty implements IdeaCylinder {
    private final ProjectRepository projects;

    public C06Novelty(ProjectRepository projects) {
        this.projects = projects;
    }

    public String name() { return "06-novelty"; }

    public void fire(IdeaContext ctx) {
        List<Project> all = projects.findAll();
        if (all.isEmpty()) return;
        double[] sims = TextSimilarity.similarities(ctx.text, all.stream().map(Project::getCorpus).toList());
        int best = 0;
        for (int i = 1; i < sims.length; i++) if (sims[i] > sims[best]) best = i;
        ctx.similarity = sims[best];
        ctx.matchedProject = all.get(best);
    }
}
