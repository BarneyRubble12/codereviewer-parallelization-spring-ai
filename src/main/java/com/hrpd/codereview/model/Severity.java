package com.hrpd.codereview.model;

/**
 * Enumeration of severity levels for code review findings.
 * 
 * <p>Severity levels are ordered from most critical to least critical,
 * allowing for prioritization of issues and filtering of results.
 * The ordering is important for deduplication logic where higher
 * severity findings take precedence over lower severity ones.</p>
 */
public enum Severity {
    
    /**
     * Critical issues that must be addressed immediately.
     * These typically represent serious security vulnerabilities or
     * code that will cause runtime failures.
     */
    BLOCKER, 
    
    /**
     * High-priority issues that should be addressed soon.
     * These represent significant problems that could impact
     * security, performance, or maintainability.
     */
    HIGH, 
    
    /**
     * Medium-priority issues that should be addressed in
     * the next development cycle. These represent moderate
     * problems that could cause issues in certain scenarios.
     */
    MEDIUM, 
    
    /**
     * Low-priority issues that can be addressed when time permits.
     * These represent minor problems or style violations that
     * don't significantly impact functionality.
     */
    LOW, 
    
    /**
     * Informational findings that provide suggestions for
     * improvement but don't represent actual problems.
     * These are typically best practices or optimization opportunities.
     */
    INFO
}
