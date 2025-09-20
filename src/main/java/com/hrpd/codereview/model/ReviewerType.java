package com.hrpd.codereview.model;

/**
 * Enumeration of different AI-powered reviewer personas available in the system.
 * 
 * <p>Each reviewer type represents a specialized AI agent that focuses on
 * a specific aspect of code quality. The system can run multiple reviewers
 * in parallel to provide comprehensive code analysis.</p>
 * 
 * <p>Additional reviewer types can be added as needed to expand the system's
 * capabilities (e.g., CONCURRENCY, TESTING, ACCESSIBILITY, etc.).</p>
 */
public enum ReviewerType {
    
    /**
     * Security-focused reviewer that identifies vulnerabilities, hardcoded secrets,
     * injection flaws, and other security-related issues.
     */
    SECURITY, 
    
    /**
     * Performance-focused reviewer that identifies memory leaks, inefficient
     * algorithms, N+1 queries, and other performance bottlenecks.
     */
    PERFORMANCE, 
    
    /**
     * Clean code reviewer that identifies code quality issues such as poor
     * naming, deep nesting, code duplication, and maintainability concerns.
     */
    CLEAN_CODE
}
