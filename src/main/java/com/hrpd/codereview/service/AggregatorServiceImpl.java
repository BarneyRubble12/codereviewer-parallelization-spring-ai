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
 * Dedupe + summarize findings from multiple reviewers.
 */
@Slf4j
public class AggregatorServiceImpl implements AggregatorService {

    @Override
    public ReviewResult merge(List<ReviewResult> parts) {
        log.info("🔗 Starting aggregation of {} review results", parts.size());
        
        // Log individual reviewer results
        for (int i = 0; i < parts.size(); i++) {
            var part = parts.get(i);
            log.debug("📊 Reviewer {}: {} findings - {}", i + 1, part.findings().size(), part.summary());
        }
        
        var all = parts.stream().flatMap(r -> r.findings().stream()).toList();
        log.info("📋 Total findings before deduplication: {}", all.size());
        
        var deduped = dedupe(all);
        log.info("🔄 Deduplication complete: {} unique findings (removed {} duplicates)", 
                deduped.size(), all.size() - deduped.size());
        
        var summary = summarize(deduped);
        log.info("📈 Final aggregated result: {} findings - {}", deduped.size(), summary);
        
        return new ReviewResult(deduped, summary);
    }

    private List<Finding> dedupe(List<Finding> in) {
        log.debug("🔍 Starting deduplication of {} findings", in.size());
        Map<String, Finding> byKey = new HashMap<>();
        int duplicatesFound = 0;
        
        for (var f : in) {
            var key = f.filePath() + "#" + f.lineStart() + "-" + f.lineEnd() + "#" + normalize(f.title());
            var existing = byKey.get(key);
            if (existing != null) {
                duplicatesFound++;
                log.debug("🔄 Found duplicate finding: {} - keeping higher severity", f.title());
            }
            byKey.merge(key, f, this::preferHigherSeverity);
        }
        
        log.debug("✅ Deduplication complete: {} duplicates found and resolved", duplicatesFound);
        return new ArrayList<>(byKey.values());
    }

    private Finding preferHigherSeverity(Finding a, Finding b) {
        var order = List.of(Severity.BLOCKER, Severity.HIGH, Severity.MEDIUM, Severity.LOW, Severity.INFO);
        return order.indexOf(a.severity()) <= order.indexOf(b.severity()) ? a : b;
    }

    private String normalize(String s) { return s == null ? "" : s.toLowerCase().replaceAll("\\s+", " "); }

    private String summarize(List<Finding> fs) {
        long blockers = fs.stream().filter(f -> f.severity() == Severity.BLOCKER).count();
        long highs = fs.stream().filter(f -> f.severity() == Severity.HIGH).count();
        return "Findings: " + fs.size() + " (BLOCKER=" + blockers + ", HIGH=" + highs + ")";
    }
}
