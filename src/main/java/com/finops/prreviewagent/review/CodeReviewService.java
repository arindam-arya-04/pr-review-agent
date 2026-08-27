package com.finops.prreviewagent.review;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Reviews a PR diff, augmented with relevant repo context (RAG), and protected
 * by resilience policies (retry + circuit breaker) around the LLM call.
 */
@Service
public class CodeReviewService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final Retry retry;
    private final CircuitBreaker circuitBreaker;

    public CodeReviewService(ChatClient.Builder builder,
                             VectorStore vectorStore,
                             Retry externalCallRetry,
                             CircuitBreaker externalCallCircuitBreaker) {
        this.chatClient = builder.build();
        this.vectorStore = vectorStore;
        this.retry = externalCallRetry;
        this.circuitBreaker = externalCallCircuitBreaker;
    }

    private static final String SYSTEM_PROMPT = """
            You are an experienced senior software engineer reviewing a GitHub pull request.
            You are given a unified diff of the changes, plus some RELATED CODE from
            elsewhere in the same repository (retrieved for context).

            Use the related code to catch cross-file problems: e.g. if the diff renames or
            changes a method that the related code calls, flag that the caller will break.

            Review for:
              - Bugs or logic errors (including breakage in OTHER files shown in the context)
              - Security issues
              - Missing error handling or edge cases
              - Code style and readability
              - Missing tests

            Be concise and specific. Mention the file and what to fix. If the change is safe,
            say so briefly. Do not invent problems that aren't there.
            """;

    public String review(String diff) {
        // 1. Retrieve related code chunks (RAG).
        List<Document> related = vectorStore.similaritySearch(
                SearchRequest.builder().query(diff).topK(4).build()
        );
        String context = related.stream()
                .map(doc -> {
                    String path = String.valueOf(doc.getMetadata().getOrDefault("path", "unknown"));
                    return "// From file: " + path + "\n" + doc.getText();
                })
                .collect(Collectors.joining("\n\n---\n\n"));

        String userMessage = """
                Here is the pull request diff to review:

                %s

                RELATED CODE FROM THE REPOSITORY (for context):

                %s
                """.formatted(diff, context.isBlank() ? "(no related code found)" : context);

        // 2. The actual LLM call, as a supplier we can decorate.
        Supplier<String> llmCall = () -> chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(userMessage)
                .call()
                .content();

        // 3. Wrap the call: circuit breaker first, then retry around it.
        //    decorateSupplier layers the policies onto the call.
        Supplier<String> resilientCall = Retry.decorateSupplier(retry,
                CircuitBreaker.decorateSupplier(circuitBreaker, llmCall));

        return resilientCall.get();
    }
}
