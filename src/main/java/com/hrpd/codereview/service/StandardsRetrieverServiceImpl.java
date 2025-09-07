package com.hrpd.codereview.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Top-K semantic search with optional metadata filter by category.
 */
public class StandardsRetrieverServiceImpl implements StandardsRetrieverService {

    private final VectorStore vectorStore;

    public StandardsRetrieverServiceImpl(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public String retrieveContext(String query, int topK, String categoryHint) {
        SearchRequest req = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .build();
        if (categoryHint != null && !categoryHint.isBlank()) {
            req = SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .filterExpression("metadata.category == '" + categoryHint + "'")
                    .build();
        }
        List<Document> docs = vectorStore.similaritySearch(req);
        return docs.stream().map(d -> "- " + d.getText()).collect(Collectors.joining("\n"));
    }
}
