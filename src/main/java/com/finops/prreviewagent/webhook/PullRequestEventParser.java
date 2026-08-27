package com.finops.prreviewagent.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finops.prreviewagent.domain.PullRequestEvent;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Turns GitHub's raw pull_request webhook JSON into our PullRequestEvent object.
 *
 * The payload is large and deeply nested; we only pull out the few fields we care
 * about. Jackson's ObjectMapper lets us navigate the JSON tree by field name.
 */
@Component
public class PullRequestEventParser {

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Parses the payload. Returns empty if this isn't a pull_request event
     * we want to store (e.g. a "ping"), so the caller can skip it cleanly.
     */
    public Optional<PullRequestEvent> parse(String eventType, String payload) {
        // We only care about pull_request events here.
        if (!"pull_request".equals(eventType)) {
            return Optional.empty();
        }

        try {
            JsonNode root = mapper.readTree(payload);

            String action = root.path("action").asText();          // e.g. "opened"
            JsonNode pr = root.path("pull_request");
            int number = pr.path("number").asInt();                 // PR number
            String title = pr.path("title").asText();               // PR title
            String author = pr.path("user").path("login").asText(); // who opened it
            String repoFullName = root.path("repository")
                                      .path("full_name").asText();  // "owner/repo"

            PullRequestEvent event = new PullRequestEvent(
                    UUID.randomUUID(),
                    repoFullName,
                    number,
                    title,
                    author,
                    action,
                    Instant.now()
            );
            return Optional.of(event);

        } catch (Exception e) {
            System.out.println(">>> Failed to parse pull_request payload: " + e.getMessage());
            return Optional.empty();
        }
    }
}
