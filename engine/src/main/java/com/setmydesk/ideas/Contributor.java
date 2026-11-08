package com.setmydesk.ideas;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import java.time.Instant;

@Entity
public class Contributor {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private String displayName;

    /** E.164 phone number. Never exposed by the public API. */
    @Column(unique = true)
    private String phone;

    private Instant joinedAt = Instant.now();

    protected Contributor() {
    }

    public Contributor(String displayName, String phone) {
        this.displayName = displayName;
        this.phone = phone;
    }

    public Long getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getPhone() { return phone; }
    public Instant getJoinedAt() { return joinedAt; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
}
