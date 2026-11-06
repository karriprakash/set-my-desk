package com.setmydesk.shares;

import com.setmydesk.ideas.Contributor;
import com.setmydesk.ideas.Idea;
import com.setmydesk.ideas.Project;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import java.time.Instant;

/** Append-only ledger row. Shares are never edited or deleted, only added. */
@Entity
public class ShareEntry {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(optional = false)
    private Project project;

    @ManyToOne(optional = false)
    private Contributor contributor;

    @ManyToOne(optional = false)
    private Idea idea;

    private double points;

    @Column(length = 500)
    private String reason;

    private Instant createdAt = Instant.now();

    protected ShareEntry() {
    }

    public ShareEntry(Project project, Contributor contributor, Idea idea, double points, String reason) {
        this.project = project;
        this.contributor = contributor;
        this.idea = idea;
        this.points = points;
        this.reason = reason;
    }

    public Long getId() { return id; }
    public Project getProject() { return project; }
    public Contributor getContributor() { return contributor; }
    public Idea getIdea() { return idea; }
    public double getPoints() { return points; }
    public String getReason() { return reason; }
    public Instant getCreatedAt() { return createdAt; }
}
