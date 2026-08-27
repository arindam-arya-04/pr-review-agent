package com.finops.prreviewagent.repository;

import com.finops.prreviewagent.domain.PullRequestEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Data-access layer for pull-request events.
 *
 * By extending JpaRepository<PullRequestEvent, UUID>, Spring generates the
 * implementation at runtime. We instantly get save(), findAll(), findById(),
 * deleteById(), etc. — no SQL to write.
 *
 * The two type parameters mean:
 *   - PullRequestEvent = the entity this repository manages
 *   - UUID             = the type of that entity's @Id (primary key)
 */
public interface PullRequestEventRepository extends JpaRepository<PullRequestEvent, UUID> {

    // A "derived query": Spring reads this method name and writes the SQL for us —
    // "select * from pull_request_events where repo_full_name = ?"
    List<PullRequestEvent> findByRepoFullName(String repoFullName);
}
