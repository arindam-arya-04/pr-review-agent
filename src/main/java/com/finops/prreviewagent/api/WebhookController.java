package com.finops.prreviewagent.api;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finops.prreviewagent.domain.PullRequestEvent;
import com.finops.prreviewagent.repository.PullRequestEventRepository;
import com.finops.prreviewagent.review.AsyncReviewRunner;
import com.finops.prreviewagent.webhook.PullRequestEventParser;
import com.finops.prreviewagent.webhook.WebhookVerifier;

@RestController
@RequestMapping("/webhook")
public class WebhookController {

    private final WebhookVerifier verifier;
    private final PullRequestEventParser parser;
    private final PullRequestEventRepository repository;
    private final AsyncReviewRunner reviewRunner;
    private final ObjectMapper mapper = new ObjectMapper();

    public WebhookController(WebhookVerifier verifier,
                             PullRequestEventParser parser,
                             PullRequestEventRepository repository,
                             AsyncReviewRunner reviewRunner) {
        this.verifier = verifier;
        this.parser = parser;
        this.repository = repository;
        this.reviewRunner = reviewRunner;
    }

    @PostMapping
    public ResponseEntity<String> receive(
            @RequestHeader(value = "X-GitHub-Event", required = false) String eventType,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody String payload
    ) {
        //  Verify 
        if (!verifier.isValid(payload, signature)) {
            System.out.println(">>> REJECTED webhook: invalid signature");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("invalid signature");
        }

        //  Parse  event.
        Optional<PullRequestEvent> parsed = parser.parse(eventType, payload);
        if (parsed.isEmpty()) {
            return ResponseEntity.ok("ignored");
        }
        PullRequestEvent event = parsed.get();

        //  Store it.
        repository.save(event);

        //  Only auto-review on newly opened , reopened PRs.
        if (!"opened".equals(event.getAction()) && !"reopened".equals(event.getAction())) {
            return ResponseEntity.ok("stored");
        }

        
        String installationId = extractInstallationId(payload);
        if (installationId == null) {
            System.out.println(">>> No installation id in payload; cannot review PR #" + event.getPrNumber());
            return ResponseEntity.ok("no-installation");
        }

        
        reviewRunner.runReview(event.getRepoFullName(), event.getPrNumber(), installationId);
        System.out.println(">>> Accepted PR #" + event.getPrNumber() + " for async review");

        return ResponseEntity.ok("accepted");
    }

    private String extractInstallationId(String payload) {
        try {
            JsonNode root = mapper.readTree(payload);
            JsonNode id = root.path("installation").path("id");
            return id.isMissingNode() || id.isNull() ? null : id.asText();
        } catch (Exception e) {
            return null;
        }
    }
}
