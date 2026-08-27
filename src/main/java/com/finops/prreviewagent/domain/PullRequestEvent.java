package com.finops.prreviewagent.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * One record per pull-request event we receive from GitHub.
 * This is a JPA @Entity, so Hibernate maps it to a database table automatically.
 * Each field below becomes a column in the "pull_request_events" table.
 */
@Entity
@Table(name = "pull_request_events")
public class PullRequestEvent {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "repo_full_name", nullable = false)
    private String repoFullName;

    @Column(name = "pr_number", nullable = false)
    private int prNumber;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String author;

    @Column(nullable = false)
    private String action;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    protected PullRequestEvent() {
    }

    public PullRequestEvent(UUID id, String repoFullName, int prNumber, String title,
                            String author, String action, Instant receivedAt) {
        this.id = id;
        this.repoFullName = repoFullName;
        this.prNumber = prNumber;
        this.title = title;
        this.author = author;
        this.action = action;
        this.receivedAt = receivedAt;
    }

    public UUID getId() { return id; }
    public String getRepoFullName() { return repoFullName; }
    public int getPrNumber() { return prNumber; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getAction() { return action; }
    public Instant getReceivedAt() { return receivedAt; }
}
