package com.hrpd.codereview.reviewer;

import com.hrpd.codereview.model.DiffHunk;
import com.hrpd.codereview.model.ReviewResult;
import com.hrpd.codereview.model.ReviewerType;

import java.util.List;

/**
 * Contract for AI-powered code reviewers that analyze code for specific types of issues.
 * 
 * <p>This interface defines the contract that all reviewer implementations must follow.
 * Each reviewer is specialized in a particular aspect of code quality (security,
 * performance, clean code, etc.) and uses AI models to analyze code changes.</p>
 * 
 * <p>Reviewers are designed to be stateless and thread-safe, allowing them to be
 * executed in parallel for optimal performance. They receive a list of diff hunks
 * and return a comprehensive review result containing all findings.</p>
 * 
 * <p>Implementations should focus on a specific domain of expertise and provide
 * detailed, actionable feedback for developers. The review process typically
 * includes grounding the AI analysis with organizational coding standards.</p>
 * 
 * @see SecurityReviewer
 * @see PerformanceReviewer
 * @see CleanCodeReviewer
 * @see ReviewerType
 * @see ReviewResult
 */
public interface Reviewer {
    
    /**
     * Returns the type of this reviewer, indicating its area of expertise.
     * 
     * @return the ReviewerType that identifies this reviewer's specialization
     */
    ReviewerType type();
    
    /**
     * Performs a comprehensive review of the provided diff hunks.
     * 
     * <p>This method analyzes each diff hunk for issues relevant to this reviewer's
     * area of expertise. The analysis typically involves:
     * <ol>
     *   <li>Retrieving relevant standards from the knowledge base</li>
     *   <li>Analyzing each hunk with AI-powered analysis</li>
     *   <li>Generating specific findings with rationale and suggestions</li>
     *   <li>Returning a comprehensive result with all findings</li>
     * </ol></p>
     * 
     * @param hunks the list of diff hunks to review
     * @return a ReviewResult containing all findings and a summary
     */
    ReviewResult review(List<DiffHunk> hunks);
}
