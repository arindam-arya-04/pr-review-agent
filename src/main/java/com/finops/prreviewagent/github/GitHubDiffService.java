package com.finops.prreviewagent.github;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;


@Service
public class GitHubDiffService {

    private final GitHubAuthService authService;
    private final RestClient restClient = RestClient.create();
    private final Retry retry;
    private final CircuitBreaker circuitBreaker;

    public GitHubDiffService(GitHubAuthService authService,
                             Retry externalCallRetry,
                             CircuitBreaker externalCallCircuitBreaker) {
        this.authService = authService;
        this.retry = externalCallRetry;
        this.circuitBreaker = externalCallCircuitBreaker;
    }

    public String fetchDiff(String repoFullName, int prNumber, String installationId) {
        String token = authService.getInstallationToken(installationId);
        String[] parts = repoFullName.split("/", 2);
        String owner = parts[0];
        String repo = parts[1];

        Supplier<byte[]> call = () -> restClient.get()
                .uri("https://api.github.com/repos/{owner}/{repo}/pulls/{number}",
                        owner, repo, prNumber)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header("Accept", "application/vnd.github.v3.diff")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .retrieve()
                .body(byte[].class);

        Supplier<byte[]> resilientCall = Retry.decorateSupplier(retry,
                CircuitBreaker.decorateSupplier(circuitBreaker, call));

        byte[] raw = resilientCall.get();
        return raw == null ? "" : new String(raw, StandardCharsets.UTF_8);
    }
}
