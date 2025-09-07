package com.hrpd.codereview.service;

/**
 * Loads standards into the vector store (called at startup).
 */
public interface StandardsIngestorService {
    void ingestFromClasspath() throws Exception;
}
