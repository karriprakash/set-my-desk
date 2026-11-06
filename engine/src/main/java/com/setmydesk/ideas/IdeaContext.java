package com.setmydesk.ideas;

import com.setmydesk.core.CombustionContext;

/** Everything one idea carries through the 12 cylinders. */
public class IdeaContext extends CombustionContext {

    // Input
    public final Contributor contributor;
    public final Channel channel;
    public String text;
    public String audioPath;
    /** Set when re-firing an idea that was waiting for transcription. */
    public Idea existing;

    // Working state, filled in by cylinders
    public Project matchedProject;
    public double similarity;
    public MeritScore merit;
    public IdeaKind kind;
    public Project project;
    public String projectTitle;
    public double points;
    public double novelty;
    public IdeaStatus status;
    public Idea idea;
    public String reply;

    public IdeaContext(Contributor contributor, Channel channel, String text, String audioPath) {
        this.contributor = contributor;
        this.channel = channel;
        this.text = text;
        this.audioPath = audioPath;
    }

    public boolean hasText() {
        return text != null && !text.isBlank();
    }
}
