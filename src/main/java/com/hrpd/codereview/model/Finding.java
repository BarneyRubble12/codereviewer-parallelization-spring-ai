package com.hrpd.codereview.model;

/**
 * A single review finding noted by a reviewer persona.
 */
public record Finding(
        String filePath,
        int lineStart,
        int lineEnd,
        String title,
        String rationale,
        String suggestion,
        Severity severity,
        ReviewerType reviewer
) {}
