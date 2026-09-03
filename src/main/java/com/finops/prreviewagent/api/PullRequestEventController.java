package com.finops.prreviewagent.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.finops.prreviewagent.domain.PullRequestEvent;
import com.finops.prreviewagent.repository.PullRequestEventRepository;

// 
//   REST endpoints for pull-request events.
 
//  @RestController      = this class handles web requests and returns data (JSON).
//  @RequestMapping("/api/events") = every URL here starts with /api/events.
//  
@RestController
@RequestMapping("/api/events")
public class PullRequestEventController {

    private final PullRequestEventRepository repository;

    // Spring automatically hands us the repository (constructor injection).
    public PullRequestEventController(PullRequestEventRepository repository) {
        this.repository = repository;
    }

    
    @GetMapping
    public List<PullRequestEvent> listAll() {
        return repository.findAll();
    }

    
    @PostMapping
    public PullRequestEvent create(@RequestBody CreateEventRequest request) {
        PullRequestEvent event = new PullRequestEvent(
                UUID.randomUUID(),
                request.repoFullName(),
                request.prNumber(),
                request.title(),
                request.author(),
                request.action(),
                Instant.now()
        );
        return repository.save(event);
    }

    public record CreateEventRequest(
            String repoFullName,
            int prNumber,
            String title,
            String author,
            String action
    ) {}
}
