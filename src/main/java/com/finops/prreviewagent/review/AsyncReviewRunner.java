package com.finops.prreviewagent.review;

import com.finops.prreviewagent.github.GitHubCommentService;
import com.finops.prreviewagent.github.GitHubDiffService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Runs the review pipeline on a background thread, for a specific installation.
 */
@Service
public class AsyncReviewRunner {

    private final GitHubDiffService diffService;
    private final CodeReviewService reviewService;
    private final GitHubCommentService commentService;

    public AsyncReviewRunner(GitHubDiffService diffService,
                             CodeReviewService reviewService,
                             GitHubCommentService commentService) {
        this.diffService = diffService;
        this.reviewService = reviewService;
        this.commentService = commentService;
    }

    @Async("reviewExecutor")
    public void runReview(String repoFullName, int prNumber, String installationId) {
        try {
            System.out.println(">>> [async] Reviewing " + repoFullName + " PR #" + prNumber
                    + " (installation " + installationId + ") on thread " + Thread.currentThread().getName());
            String diff = diffService.fetchDiff(repoFullName, prNumber, installationId);
            String review = reviewService.review(diff);
            String body = "## 🤖 AI Code Review\n\n" + review;
            commentService.postComment(repoFullName, prNumber, body, installationId);
            System.out.println(">>> [async] Review posted for PR #" + prNumber);
        } catch (Exception e) {
            System.out.println(">>> [async] Review failed for PR #" + prNumber + ": " + e.getMessage());
        }
    }
}
