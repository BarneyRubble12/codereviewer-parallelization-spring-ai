package com.hrpd.codereview.service;

/**
 * Loads standards into the vector store (called at startup).
 */
public interface StandardsIngestorService {
    void ingestFromClasspath() throws Exception;
    
    /**
     * Clears all existing documents and re-ingests everything from classpath.
     * Useful for development or when you want to force a complete refresh.
     */
    void reingestAll() throws Exception;
}
