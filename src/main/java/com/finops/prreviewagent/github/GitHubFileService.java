package com.finops.prreviewagent.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Fetches the source files of a repository from GitHub, so we can index them
 * for RAG (retrieval-augmented generation).
 *
 * We use GitHub's Git Trees API to list every file in the repo in one call,
 * then fetch the content of each code file we care about. Content comes back
 * base64-encoded, which we decode to plain text.
 */
@Service
public class GitHubFileService {

    private final GitHubAuthService authService;
    private final RestClient restClient = RestClient.create();
    private final ObjectMapper mapper = new ObjectMapper();

    public GitHubFileService(GitHubAuthService authService) {
        this.authService = authService;
    }

    /** A single file: its path in the repo and its text content. */
    public record RepoFile(String path, String content) {}

    /**
     * Returns the text content of all code files in the repo's default branch.
     */
    public List<RepoFile> fetchAllCodeFiles(String repoFullName) {
        String token = authService.getInstallationToken();
        String[] parts = repoFullName.split("/", 2);
        String owner = parts[0];
        String repo = parts[1];

        // 1. Get the whole file tree in one recursive call (branch: main).
        String treeJson = restClient.get()
                .uri("https://api.github.com/repos/{owner}/{repo}/git/trees/main?recursive=1",
                        owner, repo)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .retrieve()
                .body(String.class);

        List<RepoFile> files = new ArrayList<>();
        try {
            JsonNode tree = mapper.readTree(treeJson).path("tree");
            for (JsonNode node : tree) {
                String path = node.path("path").asText();
                String type = node.path("type").asText();
                // Only real files ("blob"), and only code-ish files.
                if ("blob".equals(type) && isCodeFile(path)) {
                    String content = fetchFileContent(owner, repo, path, token);
                    if (content != null && !content.isBlank()) {
                        files.add(new RepoFile(path, content));
                    }
                }
            }
        } catch (Exception e) {
            System.out.println(">>> Failed to list repo tree: " + e.getMessage());
        }
        return files;
    }

    private String fetchFileContent(String owner, String repo, String path, String token) {
        try {
            String json = restClient.get()
                    .uri("https://api.github.com/repos/{owner}/{repo}/contents/{path}",
                            owner, repo, path)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .header("Accept", "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .retrieve()
                    .body(String.class);

            JsonNode node = mapper.readTree(json);
            String encoded = node.path("content").asText().replaceAll("\\s", "");
            byte[] decoded = Base64.getDecoder().decode(encoded);
            return new String(decoded);
        } catch (Exception e) {
            System.out.println(">>> Failed to fetch " + path + ": " + e.getMessage());
            return null;
        }
    }

    private boolean isCodeFile(String path) {
        return path.endsWith(".java")
                || path.endsWith(".py")
                || path.endsWith(".js")
                || path.endsWith(".ts")
                || path.endsWith(".go");
    }
}
