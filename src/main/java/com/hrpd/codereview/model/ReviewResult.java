package com.hrpd.codereview.model;

import java.util.List;

/**
 * Represents the complete output from a code review operation.
 * 
 * <p>A ReviewResult contains all findings discovered during the review process
 * along with a summary of the review. This can be the output from a single
 * reviewer (security, performance, clean code) or the aggregated result from
 * multiple reviewers after deduplication and merging.</p>
 * 
 * <p>The findings list contains all identified issues, while the summary provides
 * a high-level overview of the review results, typically including counts by
 * severity level.</p>
 * 
 * @param findings the list of all findings discovered during the review
 * @param summary a brief summary of the review results and statistics
 * 
 * @see Finding
 */
public record ReviewResult(List<Finding> findings, String summary) {
    
    /**
     * Creates an empty ReviewResult with no findings and an empty summary.
     * 
     * <p>This is commonly used as a fallback when a reviewer fails or when
     * no issues are found during the review process.</p>
     * 
     * @return an empty ReviewResult instance
     */
    public static ReviewResult empty() { 
        return new ReviewResult(List.of(), ""); 
    }
}

