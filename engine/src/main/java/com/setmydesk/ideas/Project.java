package com.setmydesk.ideas;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;

import java.time.Instant;

@Entity
public class Project {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private String title;

    /** Founding idea plus every accepted enhancement; this is what new ideas are compared against. */
    @Lob
    @Column(nullable = false)
    private String corpus;

    @ManyToOne(optional = false)
    private Contributor founder;

    private double totalPoints;

    private Instant createdAt = Instant.now();

    protected Project() {
    }

    public Project(String title, String foundingText, Contributor founder) {
        this.title = title;
        this.corpus = foundingText;
        this.founder = founder;
    }

    public void absorb(String enhancementText, double points) {
        if (enhancementText != null && !enhancementText.isBlank()) this.corpus = corpus + "\n" + enhancementText;
        this.totalPoints += points;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getCorpus() { return corpus; }
    public Contributor getFounder() { return founder; }
    public double getTotalPoints() { return totalPoints; }
    public Instant getCreatedAt() { return createdAt; }
}
