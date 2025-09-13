package com.hrpd.codereview.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Top-K semantic search with optional metadata filter by category.
 */
@Slf4j
@RequiredArgsConstructor
public class StandardsRetrieverServiceImpl implements StandardsRetrieverService {

    private final VectorStore vectorStore;

    @Override
    public String retrieveContext(String query, int topK, String categoryHint) {
        log.debug("🔍 Retrieving context: query='{}', topK={}, category='{}'", query, topK, categoryHint);
        
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
            log.debug("🎯 Using category filter: {}", categoryHint);
        }
        
        List<Document> docs = vectorStore.similaritySearch(req);
        String context = docs.stream().map(d -> "- " + d.getText()).collect(Collectors.joining("\n"));
        
        log.debug("📚 Retrieved {} documents, context length: {} characters", docs.size(), context.length());
        return context;
    }
}
