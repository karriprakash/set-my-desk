package com.setmydesk.ideas;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;

import java.time.Instant;

@Entity
public class Idea {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(optional = false)
    private Contributor contributor;

    @ManyToOne
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Channel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IdeaStatus status;

    @Enumerated(EnumType.STRING)
    private IdeaKind kind;

    @Lob
    private String text;

    private String audioPath;
    private Double similarity;
    private Double merit;
    private Double points;

    @Column(length = 1000)
    private String feedback;

    private Instant createdAt = Instant.now();

    protected Idea() {
    }

    public Idea(Contributor contributor, Channel channel) {
        this.contributor = contributor;
        this.channel = channel;
        this.status = IdeaStatus.PENDING_TRANSCRIPTION;
    }

    public Long getId() { return id; }
    public Contributor getContributor() { return contributor; }
    public Project getProject() { return project; }
    public Channel getChannel() { return channel; }
    public IdeaStatus getStatus() { return status; }
    public IdeaKind getKind() { return kind; }
    public String getText() { return text; }
    public String getAudioPath() { return audioPath; }
    public Double getSimilarity() { return similarity; }
    public Double getMerit() { return merit; }
    public Double getPoints() { return points; }
    public String getFeedback() { return feedback; }
    public Instant getCreatedAt() { return createdAt; }

    public void setProject(Project project) { this.project = project; }
    public void setStatus(IdeaStatus status) { this.status = status; }
    public void setKind(IdeaKind kind) { this.kind = kind; }
    public void setText(String text) { this.text = text; }
    public void setAudioPath(String audioPath) { this.audioPath = audioPath; }
    public void setSimilarity(Double similarity) { this.similarity = similarity; }
    public void setMerit(Double merit) { this.merit = merit; }
    public void setPoints(Double points) { this.points = points; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
}
