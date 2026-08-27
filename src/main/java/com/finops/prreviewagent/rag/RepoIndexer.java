package com.finops.prreviewagent.rag;

import com.finops.prreviewagent.github.GitHubFileService;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Indexes a repository's code into the vector store for RAG.
 *
 * Indexing is expensive (fetch every file + embed every chunk via OpenAI), so
 * we cache the result per repo with @Cacheable. A repeated index request for the
 * same repo returns the cached chunk-count without re-fetching or re-embedding.
 *
 * (In-memory cache locally; swappable to Redis in production. To force a
 * re-index when code changes, we'd evict this cache — a good future enhancement.)
 */
@Service
public class RepoIndexer {

    private final GitHubFileService fileService;
    private final VectorStore vectorStore;

    private static final int CHUNK_SIZE = 1500;

    public RepoIndexer(GitHubFileService fileService, VectorStore vectorStore) {
        this.fileService = fileService;
        this.vectorStore = vectorStore;
    }

    @Cacheable(value = "repoIndex", key = "#repoFullName")
    public int indexRepo(String repoFullName) {
        List<GitHubFileService.RepoFile> files = fileService.fetchAllCodeFiles(repoFullName);
        List<Document> documents = new ArrayList<>();

        for (GitHubFileService.RepoFile file : files) {
            List<String> chunks = chunk(file.content(), CHUNK_SIZE);
            for (int i = 0; i < chunks.size(); i++) {
                Document doc = new Document(
                        chunks.get(i),
                        Map.of(
                                "repo", repoFullName,
                                "path", file.path(),
                                "chunk", String.valueOf(i)
                        )
                );
                documents.add(doc);
            }
        }

        if (!documents.isEmpty()) {
            vectorStore.add(documents);
        }
        System.out.println(">>> Indexed " + documents.size() + " chunks from "
                + files.size() + " files in " + repoFullName);
        return documents.size();
    }

    private List<String> chunk(String text, int maxChars) {
        List<String> chunks = new ArrayList<>();
        String[] lines = text.split("\n");
        StringBuilder current = new StringBuilder();
        for (String line : lines) {
            if (current.length() + line.length() + 1 > maxChars && current.length() > 0) {
                chunks.add(current.toString());
                current = new StringBuilder();
            }
            current.append(line).append("\n");
        }
        if (current.length() > 0) {
            chunks.add(current.toString());
        }
        return chunks;
    }
}
