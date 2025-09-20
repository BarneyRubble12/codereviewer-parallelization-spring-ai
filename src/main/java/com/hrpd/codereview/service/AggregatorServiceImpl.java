package com.hrpd.codereview.service;

import com.hrpd.codereview.model.Finding;
import com.hrpd.codereview.model.ReviewResult;
import com.hrpd.codereview.model.Severity;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the aggregator service that merges and deduplicates findings from multiple reviewers.
 * 
 * <p>This service is responsible for combining results from different AI-powered reviewers
 * (security, performance, clean code) into a single, coherent review result. It handles
 * the complex task of identifying and resolving duplicate findings while preserving
 * the most relevant information.</p>
 * 
 * <p>The aggregation process includes:
 * <ol>
 *   <li>Collecting all findings from multiple reviewers</li>
 *   <li>Identifying duplicate findings based on location and content</li>
 *   <li>Resolving conflicts by preferring higher severity findings</li>
 *   <li>Generating a comprehensive summary with statistics</li>
 * </ol></p>
 * 
 * <p>Deduplication is performed using a composite key based on file path, line range,
 * and normalized title content. When duplicates are found, the finding with higher
 * severity is retained.</p>
 * 
 * @see AggregatorService
 * @see Finding
 * @see ReviewResult
 */
@Slf4j
public class AggregatorServiceImpl implements AggregatorService {

    /**
     * Merges multiple review results into a single, deduplicated result.
     * 
     * <p>This method combines findings from multiple reviewers, removes duplicates,
     * and generates a comprehensive summary. The process includes detailed logging
     * for monitoring and debugging purposes.</p>
     * 
     * @param parts the list of review results from different reviewers
     * @return a merged ReviewResult with deduplicated findings and summary
     */
    @Override
    public ReviewResult merge(List<ReviewResult> parts) {
        log.info("🔗 Starting aggregation of {} review results", parts.size());
        
        // Log individual reviewer results for monitoring
        for (int i = 0; i < parts.size(); i++) {
            var part = parts.get(i);
            log.debug("📊 Reviewer {}: {} findings - {}", i + 1, part.findings().size(), part.summary());
        }
        
        // Flatten all findings from all reviewers into a single list
        var all = parts.stream().flatMap(r -> r.findings().stream()).toList();
        log.info("📋 Total findings before deduplication: {}", all.size());
        
        // Remove duplicate findings and resolve conflicts
        var deduped = dedupe(all);
        log.info("🔄 Deduplication complete: {} unique findings (removed {} duplicates)", 
                deduped.size(), all.size() - deduped.size());
        
        // Generate comprehensive summary with statistics
        var summary = summarize(deduped);
        log.info("📈 Final aggregated result: {} findings - {}", deduped.size(), summary);
        
        return new ReviewResult(deduped, summary);
    }

    /**
     * Removes duplicate findings and resolves conflicts by preferring higher severity.
     * 
     * <p>Deduplication is performed using a composite key that includes:
     * <ul>
     *   <li>File path</li>
     *   <li>Line range (start-end)</li>
     *   <li>Normalized title content</li>
     * </ul></p>
     * 
     * <p>When duplicate findings are identified, the one with higher severity is retained.
     * This ensures that critical issues are not lost during the deduplication process.</p>
     * 
     * @param in the list of findings to deduplicate
     * @return a deduplicated list of findings
     */
    private List<Finding> dedupe(List<Finding> in) {
        log.debug("🔍 Starting deduplication of {} findings", in.size());
        Map<String, Finding> byKey = new HashMap<>();
        int duplicatesFound = 0;
        
        for (var f : in) {
            // Create composite key for deduplication
            var key = f.filePath() + "#" + f.lineStart() + "-" + f.lineEnd() + "#" + normalize(f.title());
            var existing = byKey.get(key);
            if (existing != null) {
                duplicatesFound++;
                log.debug("🔄 Found duplicate finding: {} - keeping higher severity", f.title());
            }
            // Merge findings, preferring the one with higher severity
            byKey.merge(key, f, this::preferHigherSeverity);
        }
        
        log.debug("✅ Deduplication complete: {} duplicates found and resolved", duplicatesFound);
        return new ArrayList<>(byKey.values());
    }

    /**
     * Compares two findings and returns the one with higher severity.
     * 
     * <p>This method is used during deduplication to resolve conflicts when
     * multiple reviewers identify the same issue. The severity order is:
     * BLOCKER > HIGH > MEDIUM > LOW > INFO.</p>
     * 
     * @param a the first finding to compare
     * @param b the second finding to compare
     * @return the finding with higher severity, or the first one if equal
     */
    private Finding preferHigherSeverity(Finding a, Finding b) {
        var order = List.of(Severity.BLOCKER, Severity.HIGH, Severity.MEDIUM, Severity.LOW, Severity.INFO);
        return order.indexOf(a.severity()) <= order.indexOf(b.severity()) ? a : b;
    }

    /**
     * Normalizes a string for comparison purposes during deduplication.
     * 
     * <p>This method converts the string to lowercase and collapses multiple
     * whitespace characters into single spaces. This helps identify findings
     * that are essentially the same but may have minor formatting differences.</p>
     * 
     * @param s the string to normalize
     * @return the normalized string, or empty string if input is null
     */
    private String normalize(String s) { 
        return s == null ? "" : s.toLowerCase().replaceAll("\\s+", " "); 
    }

    /**
     * Generates a summary string with statistics about the findings.
     * 
     * <p>This method creates a concise summary that includes the total number
     * of findings and counts for the most critical severity levels (BLOCKER and HIGH).
     * This provides quick insight into the overall quality and urgency of issues found.</p>
     * 
     * @param fs the list of findings to summarize
     * @return a summary string with finding counts by severity
     */
    private String summarize(List<Finding> fs) {
        long blockers = fs.stream().filter(f -> f.severity() == Severity.BLOCKER).count();
        long highs = fs.stream().filter(f -> f.severity() == Severity.HIGH).count();
        return "Findings: " + fs.size() + " (BLOCKER=" + blockers + ", HIGH=" + highs + ")";
    }
}
