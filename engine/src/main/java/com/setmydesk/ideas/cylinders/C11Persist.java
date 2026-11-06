package com.setmydesk.ideas.cylinders;

import com.setmydesk.ideas.Idea;
import com.setmydesk.ideas.IdeaContext;
import com.setmydesk.ideas.IdeaKind;
import com.setmydesk.ideas.IdeaRepository;
import com.setmydesk.ideas.IdeaStatus;
import com.setmydesk.ideas.Project;
import com.setmydesk.ideas.ProjectRepository;
import com.setmydesk.shares.ShareEntry;
import com.setmydesk.shares.ShareEntryRepository;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** 11. Write the idea, the project and an append-only ledger row, in one transaction. */
@Component
@Order(11)
public class C11Persist implements IdeaCylinder {
    private final IdeaRepository ideas;
    private final ProjectRepository projects;
    private final ShareEntryRepository ledger;

    public C11Persist(IdeaRepository ideas, ProjectRepository projects, ShareEntryRepository ledger) {
        this.ideas = ideas;
        this.projects = projects;
        this.ledger = ledger;
    }

    public String name() { return "11-persist"; }

    public void fire(IdeaContext ctx) {
        Project project;
        if (ctx.kind == IdeaKind.FOUNDING) {
            project = new Project(ctx.projectTitle, ctx.text, ctx.contributor);
            project.absorb("", ctx.points);
            project = projects.save(project);
        } else {
            project = ctx.project;
            project.absorb(ctx.text, ctx.points);
            project = projects.save(project);
        }
        ctx.project = project;

        Idea idea = ctx.existing != null ? ctx.existing : new Idea(ctx.contributor, ctx.channel);
        idea.setText(ctx.text);
        idea.setAudioPath(ctx.audioPath);
        idea.setProject(project);
        idea.setKind(ctx.kind);
        idea.setStatus(IdeaStatus.ACCEPTED);
        idea.setSimilarity(round(ctx.similarity));
        idea.setMerit(ctx.merit.total());
        idea.setPoints(ctx.points);
        ctx.idea = ideas.save(idea);
        ctx.status = IdeaStatus.ACCEPTED;

        String reason = "%s · merit %.2f × novelty %.2f".formatted(
                ctx.kind == IdeaKind.FOUNDING ? "Founding idea" : "Enhancement", ctx.merit.total(), ctx.novelty);
        ledger.save(new ShareEntry(project, ctx.contributor, ctx.idea, ctx.points, reason));
    }

    private static double round(double v) {
        return Math.round(v * 1000) / 1000.0;
    }
}
