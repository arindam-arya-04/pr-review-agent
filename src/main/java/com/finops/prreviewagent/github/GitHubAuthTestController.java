package com.finops.prreviewagent.github;

import com.finops.prreviewagent.rag.RepoIndexer;
import com.finops.prreviewagent.review.CodeReviewService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * TEMPORARY test endpoints. These use our OWN default installation id for
 * manual testing; the real flow gets the installation id from each webhook.
 */
@RestController
public class GitHubAuthTestController {

    private final GitHubDiffService diffService;
    private final CodeReviewService reviewService;
    private final RepoIndexer repoIndexer;
    private final String defaultInstallationId;

    public GitHubAuthTestController(GitHubDiffService diffService,
                                    CodeReviewService reviewService,
                                    RepoIndexer repoIndexer,
                                    @Value("${github.app.installation-id}") String defaultInstallationId) {
        this.diffService = diffService;
        this.reviewService = reviewService;
        this.repoIndexer = repoIndexer;
        this.defaultInstallationId = defaultInstallationId;
    }

    @GetMapping("/test-diff")
    public String testDiff(@RequestParam String repo, @RequestParam int pr) {
        return diffService.fetchDiff(repo, pr, defaultInstallationId);
    }

    @GetMapping("/test-review")
    public String testReview(@RequestParam String repo, @RequestParam int pr) {
        String diff = diffService.fetchDiff(repo, pr, defaultInstallationId);
        return reviewService.review(diff);
    }

    @GetMapping("/test-index")
    public String testIndex(@RequestParam String repo) {
        int chunks = repoIndexer.indexRepo(repo);
        return "Indexed " + chunks + " chunks from " + repo;
    }
}
