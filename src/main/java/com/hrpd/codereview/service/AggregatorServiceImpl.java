package com.hrpd.codereview.service;

import com.hrpd.codereview.model.Finding;
import com.hrpd.codereview.model.ReviewResult;
import com.hrpd.codereview.model.Severity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dedupe + summarize findings from multiple reviewers.
 */
public class AggregatorServiceImpl implements AggregatorService {

    @Override
    public ReviewResult merge(List<ReviewResult> parts) {
        var all = parts.stream().flatMap(r -> r.findings().stream()).toList();
        var deduped = dedupe(all);
        var summary = summarize(deduped);
        return new ReviewResult(deduped, summary);
    }

    private List<Finding> dedupe(List<Finding> in) {
        Map<String, Finding> byKey = new HashMap<>();
        for (var f : in) {
            var key = f.filePath() + "#" + f.lineStart() + "-" + f.lineEnd() + "#" + normalize(f.title());
            byKey.merge(key, f, this::preferHigherSeverity);
        }
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
