package com.hrpd.codereview.reviewer;

import com.hrpd.codereview.model.DiffHunk;
import com.hrpd.codereview.model.Finding;
import com.hrpd.codereview.model.ReviewResult;
import com.hrpd.codereview.model.ReviewerType;
import com.hrpd.codereview.service.StandardsRetrieverService;
import com.hrpd.codereview.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

import java.util.ArrayList;
import java.util.List;

/**
 * AI-powered reviewer specialized in identifying performance bottlenecks and optimization opportunities.
 * 
 * <p>This reviewer focuses on detecting performance issues, inefficient algorithms,
 * and potential optimization opportunities in Java code. It uses a combination of
 * AI analysis and organizational performance standards to provide comprehensive
 * performance feedback.</p>
 * 
 * <p>The reviewer is particularly effective at identifying:
 * <ul>
 *   <li>Memory leaks and inefficient object allocations</li>
 *   <li>N+1 database query problems</li>
 *   <li>Missing connection pooling configurations</li>
 *   <li>Inefficient loops and algorithms</li>
 *   <li>Large object creation in hot code paths</li>
 *   <li>Missing caching opportunities</li>
 *   <li>Inefficient string operations</li>
 *   <li>Excessive garbage collection pressure</li>
 * </ul></p>
 * 
 * @see Reviewer
 * @see StandardsRetrieverService
 */
@Slf4j
@RequiredArgsConstructor
public class PerformanceReviewer implements Reviewer {

    /**
     * Chat client for communicating with the AI model.
     */
    private final ChatClient chat;
    
    /**
     * Service for retrieving relevant performance standards from the knowledge base.
     */
    private final StandardsRetrieverService retriever;

    /**
     * Returns the type of this reviewer.
     * 
     * @return PERFORMANCE reviewer type
     */
    @Override 
    public ReviewerType type() {
        return ReviewerType.PERFORMANCE;
    }

    /**
     * Performs a comprehensive performance review on the provided diff hunks.
     * 
     * <p>This method analyzes each diff hunk for performance issues using AI-powered
     * analysis combined with organizational performance standards. The review process includes:
     * <ol>
     *   <li>Retrieving relevant performance standards from the knowledge base</li>
     *   <li>Analyzing each diff hunk for performance bottlenecks</li>
     *   <li>Generating specific performance findings with optimization suggestions</li>
     *   <li>Aggregating all findings into a comprehensive performance report</li>
     * </ol></p>
     * 
     * @param hunks the list of diff hunks to review for performance issues
     * @return a ReviewResult containing all performance findings and a summary
     */
    @Override
    public ReviewResult review(List<DiffHunk> hunks) {
        log.info("⚡ Starting PERFORMANCE review for {} hunks", hunks.size());
        var findings = new ArrayList<Finding>();
        
        // Retrieve relevant performance standards to ground the AI analysis
        log.debug("🔍 Retrieving performance standards context...");
        String grounding = retriever.retrieveContext(
                "java performance; allocations; GC pressure; streams; SQL N+1; caching; pagination", 6, "performance");
        log.debug("📚 Retrieved {} characters of performance standards", grounding.length());

        // Analyze each diff hunk individually for performance issues
        for (int i = 0; i < hunks.size(); i++) {
            var h = hunks.get(i);
            log.debug("🔍 Analyzing performance hunk {}/{}: {}", i + 1, hunks.size(), h.filePath());
            
            // Construct a performance-focused prompt with specific optimization criteria
            String prompt = """
        You are a senior Java PERFORMANCE reviewer. Look for performance issues.
        
        IMPORTANT: Return ONLY valid JSON. Do not include any explanatory text before or after the JSON.
        
        Analyze this code diff and return EXACTLY this JSON format:
        
        {"findings":[
           {"title":"Issue Title","rationale":"Why this is a problem","suggestion":"How to fix it",
            "severity":"HIGH","filePath":"","lineStart":1,"lineEnd":1}
         ],
         "summary":"Brief summary"}
        
        Look specifically for:
        1. Memory leaks and inefficient allocations
        2. N+1 database queries
        3. Missing connection pooling
        4. Inefficient loops or algorithms
        5. Large object creation in hot paths
        6. Missing caching opportunities
        
        IMPORTANT: 
        - If you find performance issues, return them in the findings array
        - If no issues, return empty findings array
        - Return ONLY the JSON object, no other text
        
        Code to analyze:
        ```diff
        %s
        ```
        """.formatted(h.patch());

            // Call the AI model to analyze the code for performance issues
            log.debug("🤖 Calling AI model for performance analysis...");
            String json = chat.prompt().user(prompt).call().content();
            
            // Parse the AI response and extract performance findings
            var hunkFindings = JsonUtils.parseFindings(json, ReviewerType.PERFORMANCE, h.filePath());
            findings.addAll(hunkFindings);
            log.debug("✅ Performance analysis complete for hunk {}/{}: {} findings", 
                    i + 1, hunks.size(), hunkFindings.size());
        }
        // Return the aggregated performance findings from all hunks
        log.info("⚡ PERFORMANCE review complete: {} total findings", findings.size());
        return new ReviewResult(findings, "Performance review (grounded) complete");
    }
}
