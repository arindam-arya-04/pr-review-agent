package com.finops.prreviewagent.github;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Posts a review comment on a pull request, using the token for the specific
 * installation that owns the repo.
 */
@Service
public class GitHubCommentService {

    private final GitHubAuthService authService;
    private final RestClient restClient = RestClient.create();

    public GitHubCommentService(GitHubAuthService authService) {
        this.authService = authService;
    }

    public void postComment(String repoFullName, int prNumber, String body, String installationId) {
        String token = authService.getInstallationToken(installationId);
        String[] parts = repoFullName.split("/", 2);
        String owner = parts[0];
        String repo = parts[1];

        restClient.post()
                .uri("https://api.github.com/repos/{owner}/{repo}/issues/{number}/comments",
                        owner, repo, prNumber)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .body(Map.of("body", body))
                .retrieve()
                .toBodilessEntity();
    }
}
