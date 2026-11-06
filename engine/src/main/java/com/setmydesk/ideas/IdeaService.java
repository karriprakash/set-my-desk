package com.setmydesk.ideas;

import com.setmydesk.core.Crankshaft;
import com.setmydesk.ideas.cylinders.IdeaCylinder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

/** Entry point for every channel: web, app, voice note, WhatsApp, phone call. */
@Service
public class IdeaService {

    private final Crankshaft<IdeaContext> crankshaft;
    private final ContributorRepository contributors;
    private final IdeaRepository ideas;

    public IdeaService(List<IdeaCylinder> cylinders, ContributorRepository contributors, IdeaRepository ideas) {
        this.crankshaft = new Crankshaft<>(cylinders);
        this.contributors = contributors;
        this.ideas = ideas;
    }

    public record Outcome(IdeaStatus status, Long ideaId, Long projectId, String reply, List<String> trace) {
    }

    /** Ideas are processed one at a time so two near-identical ideas can't both claim "founder". */
    @Transactional
    public synchronized Outcome submit(String name, String phone, Channel channel, String text, String audioPath) {
        Contributor who = resolveContributor(name, phone);
        IdeaContext ctx = crankshaft.turn(new IdeaContext(who, channel, text, audioPath));

        if (ctx.status == IdeaStatus.PENDING_TRANSCRIPTION) {
            Idea pending = new Idea(who, channel);
            pending.setAudioPath(audioPath);
            pending.setFeedback(ctx.reply);
            ctx.idea = ideas.save(pending);
        }
        return outcome(ctx);
    }

    /** A volunteer (or a late speech-to-text job) supplies the words for a parked voice note. */
    @Transactional
    public synchronized Outcome completeTranscription(Long ideaId, String transcript) {
        Idea idea = ideas.findById(ideaId).orElseThrow(() -> new NoSuchElementException("No idea " + ideaId));
        if (idea.getStatus() != IdeaStatus.PENDING_TRANSCRIPTION) {
            throw new IllegalStateException("Idea " + ideaId + " is not waiting for transcription");
        }
        IdeaContext ctx = new IdeaContext(idea.getContributor(), idea.getChannel(), transcript, idea.getAudioPath());
        ctx.existing = idea;
        crankshaft.turn(ctx);
        if (ctx.status != IdeaStatus.ACCEPTED) {
            idea.setFeedback(ctx.reply);
            if (ctx.status != IdeaStatus.PENDING_TRANSCRIPTION) idea.setStatus(ctx.status);
            ideas.save(idea);
            ctx.idea = idea;
        }
        return outcome(ctx);
    }

    public List<String> firingOrder() {
        return crankshaft.firingOrder();
    }

    private Contributor resolveContributor(String name, String phone) {
        String display = name == null || name.isBlank() ? "Friend" : name.trim();
        if (phone == null || phone.isBlank()) return contributors.save(new Contributor(display, null));
        String normalized = phone.replaceAll("[^+\\d]", "");
        return contributors.findByPhone(normalized).map(c -> {
            if (name != null && !name.isBlank() && "Friend".equals(c.getDisplayName())) c.setDisplayName(display);
            return c;
        }).orElseGet(() -> contributors.save(new Contributor(display, normalized)));
    }

    private static Outcome outcome(IdeaContext ctx) {
        return new Outcome(ctx.status,
                ctx.idea == null ? null : ctx.idea.getId(),
                ctx.project == null ? null : ctx.project.getId(),
                ctx.reply, ctx.trace());
    }
}
