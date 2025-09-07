package com.hrpd.codereview.service;

/**
 * Retrieves top-K standards text for prompt grounding.
 */
public interface StandardsRetrieverService {
    String retrieveContext(String query, int topK, String categoryHint);
}
